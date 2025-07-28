package org.bhmc.blacklistremover.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.bhmc.blacklistremover.mapper.DeletedMacMapper;
import org.bhmc.blacklistremover.model.BlockedMac;
import org.bhmc.blacklistremover.model.Wlc;
import org.bhmc.blacklistremover.repository.BlockedMacRepository;
import org.bhmc.blacklistremover.repository.DeletedMacRepository;
import org.bhmc.blacklistremover.to.BlockedMacTo;
import org.bhmc.blacklistremover.util.arubacontroller.BlackListUtil;
import org.bhmc.blacklistremover.web.AuthUtil;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlockedMacService {

    private final BlockedMacRepository repository;
    private final DeletedMacRepository deletedMacRepository;
    private final BlackListUtil blackListUtil;
    private final DeletedMacMapper deletedMacMapper;
    private final BlockedMacSyncService syncService;


    public boolean deleteBlockedMac(int id) {
        BlockedMac mac = repository.getExisted(id);
        String deletedMacAddress = mac.getClientMac();
        int wlcId = mac.getWlc().getId();

        deletedMacRepository.save(deletedMacMapper.fromBlockedMac(mac, AuthUtil.authId()));
        try {
            // Remove from WLC hardware
            boolean wlcRemovalSuccess = blackListUtil.removeMacAddress(mac);

            if (wlcRemovalSuccess) {
                // Remove from database
                repository.delete(mac);

                // Force immediate sync to verify consistency
                //syncService.syncMacsToDatabase(blackListUtil.getBlockedMacAddresses());
                syncService.removeMacFromCache(deletedMacAddress, wlcId);
                log.info("Successfully deleted MAC {} with verification sync", deletedMacAddress);
                return true;
            } else {
                log.warn("Failed to remove MAC {} from WLC", deletedMacAddress);
                return false;
            }
        } catch (Exception e) {
            log.error("Error during MAC deletion: {}", e.getMessage(), e);
            return false;
        }
    }


    @Transactional
    public List<BlockedMac> findAll() {
        // Since SSHBlackListUtil automatically syncs data every minute,
        // we can just return from database
        syncService.syncMacsToDatabase(blackListUtil.getBlockedMacAddresses());
        return repository.findAll();
    }


    // Delegate to sync service to avoid code duplication
    @Transactional
    public Wlc getWlcById(int id) {
        return syncService.getWlcById(id);
    }

    @Transactional
    public void saveMacs(List<BlockedMac> macs) {
        repository.saveAll(macs);
    }

    @Transactional
    public BlockedMac updateBlockedMac(BlockedMac mac, BlockedMacTo macTo) {
        return syncService.updateBlockedMac(mac, macTo);
    }
}


