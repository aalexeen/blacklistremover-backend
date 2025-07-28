
package org.bhmc.blacklistremover.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.bhmc.blacklistremover.mapper.BlockedMapMapper;
import org.bhmc.blacklistremover.mapper.DeletedMacMapper;
import org.bhmc.blacklistremover.model.BlockedMac;
import org.bhmc.blacklistremover.model.DeletedMac;
import org.bhmc.blacklistremover.model.Wlc;
import org.bhmc.blacklistremover.repository.BlockedMacRepository;
import org.bhmc.blacklistremover.repository.DeletedMacRepository;
import org.bhmc.blacklistremover.repository.WlcRepository;
import org.bhmc.blacklistremover.service.ssh.SSHBlackListUtil;
import org.bhmc.blacklistremover.to.BlockedMacTo;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlockedMacSyncService {

    private final BlockedMacRepository repository;
    private final WlcRepository wlcRepository;
    private final BlockedMapMapper blockedMapMapper;
    private final DeletedMacRepository deletedMacRepository;
    private final DeletedMacMapper deletedMacMapper;

    // Use field injection with @Lazy to break circular dependency
    @Autowired
    @Lazy
    private SSHBlackListUtil sshBlackListUtil;


    /**
     * Sync MAC addresses from WLC to database - same logic as BlockedMacService.findAll()
     */
    @Transactional
    public void syncMacsToDatabase(List<BlockedMacTo> macAddressesFromWLC) {
        List<BlockedMac> savedMacs = repository.findAll();
        log.info("Saved MAC addresses: {}", savedMacs.size());

        List<BlockedMac> macs = new ArrayList<>();
        List<BlockedMac> macsToDelete = new ArrayList<>();

        for (BlockedMacTo macTo : macAddressesFromWLC) {
            if (savedMacs.stream()
                         .noneMatch(savedMac -> savedMac.getClientMac().equals(macTo.getClientMac()))) {
                macs.add(blockedMapMapper.toEntity(macTo, getWlcById(macTo.getWlcId())));
            } else {
                for (BlockedMac savedMac : savedMacs) {
                    if (savedMac.getClientMac().equals(macTo.getClientMac()) &&
                            savedMac.getWlc().getId() == (macTo.getWlcId()) &&
                            !savedMac.equals(blockedMapMapper.toEntity(macTo, getWlcById(macTo.getWlcId())))) {
                        updateBlockedMac(repository.getExisted(savedMac.getId()), macTo);
                    }
                }
            }
        }

        // Find MACs that exist in database but not in WLC anymore
        for (BlockedMac savedMac : savedMacs) {
            boolean foundInWLC = macAddressesFromWLC.stream()
                                                    .anyMatch(macTo -> macTo.getClientMac()
                                                                            .equals(savedMac.getClientMac()) &&
                                                            macTo.getWlcId() == savedMac.getWlc().getId());

            if (!foundInWLC) {
                // MAC exists in DB but not in WLC - it was removed from WLC
                macsToDelete.add(savedMac);
                log.info("MAC {} no longer exists on WLC {}, marking as deleted",
                        savedMac.getClientMac(), savedMac.getWlc().getId());
            }
        }

        saveMacs(macs);
        log.info("Synced {} new MAC addresses to database", macs.size());

        // Handle deleted MACs - use bulk operations for better performance
        if (!macsToDelete.isEmpty()) {
            // Convert BlockedMac list to DeletedMac list
            List<DeletedMac> deletedMacs = macsToDelete.stream()
                                                       .map(macToDelete -> deletedMacMapper.fromBlockedMac(macToDelete, 0))
                                                       .toList();

            // Bulk save to deleted_mac table
            deletedMacRepository.saveAll(deletedMacs);

            // Bulk delete from blocked_mac table
            repository.deleteAll(macsToDelete);

            log.info("Moved {} MAC addresses to deleted_mac table (system removal)", macsToDelete.size());

            // Log individual MACs for audit trail
            macsToDelete.forEach(mac ->
                    log.info("Moved MAC {} from WLC {} to deleted_mac table",
                            mac.getClientMac(), mac.getWlc().getId()));
        }
    }

    /**
     * Remove a specific MAC address from cache and database tracking
     * This method handles post-deletion cleanup to avoid race conditions
     */
    public void removeMacFromCache(String macAddress, int wlcId) {
        try {
            log.info("Removing MAC {} from WLC {} from cache and sync tracking", macAddress, wlcId);

            // Invalidate the cache in SSHBlackListUtil to force fresh fetch
            sshBlackListUtil.invalidateCache();

            // Remove any database entries that might have been re-added during sync
            // This handles edge cases where the MAC was re-added between deletion and sync
            List<BlockedMac> existingMacs = repository.findAll();
            existingMacs.stream()
                        .filter(mac -> mac.getClientMac().equals(macAddress) &&
                                mac.getWlc().getId() == wlcId)
                        .forEach(mac -> {
                            log.warn("Found re-added MAC {} during cleanup, removing again", macAddress);
                            repository.delete(mac);
                        });

            log.info("Successfully cleaned up MAC {} from cache and tracking", macAddress);
        } catch (Exception e) {
            log.error("Error during MAC cleanup for {}: {}", macAddress, e.getMessage(), e);
        }
    }


    @Transactional
    public Wlc getWlcById(int id) {
        return wlcRepository.findById(id)
                            .orElseThrow(() -> new IllegalArgumentException("Wlc not found with id: " + id));
    }

    @Transactional
    public void saveMacs(List<BlockedMac> macs) {
        repository.saveAll(macs);
    }

    @Transactional
    public BlockedMac updateBlockedMac(BlockedMac mac, BlockedMacTo macTo) {
        if (macTo.getBlockTime() != null && !mac.getBlockTime().equals(macTo.getBlockTime())) {
            mac.setBlockTime(macTo.getBlockTime());
        }

        if (macTo.getRemainingTime() != null && !mac.getRemainingTime().equals(macTo.getRemainingTime())) {
            mac.setRemainingTime(macTo.getRemainingTime());
        }

        if (macTo.getReason() != null && !mac.getReason().equals(macTo.getReason())) {
            mac.setReason(macTo.getReason());
        }

        if (macTo.getWlcId() != 0 && mac.getWlc().getId() != (macTo.getWlcId())) {
            mac.setWlc(getWlcById(macTo.getWlcId()));
        }

        if (mac.getUserId() != macTo.getUserId()) {
            mac.setUserId(macTo.getUserId());
        }
        return mac;
    }
}