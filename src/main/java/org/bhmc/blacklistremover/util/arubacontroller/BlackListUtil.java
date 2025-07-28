package org.bhmc.blacklistremover.util.arubacontroller;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.bhmc.blacklistremover.model.BlockedMac;
import org.bhmc.blacklistremover.service.ssh.SSHBlackListUtil;
import org.bhmc.blacklistremover.to.BlockedMacTo;

import java.util.List;

@Getter
@RequiredArgsConstructor
@Component
@Slf4j
public class BlackListUtil {

    @Autowired
    private final SSHBlackListUtil sshBlackListUtil;

    /**
     * Remove MAC address using SSH command
     */
    public boolean removeMacAddress(BlockedMac blockedMac) {
        boolean success = sshBlackListUtil.removeMacAddress(blockedMac);

        if (success) {
            log.info("Successfully removed MAC address: {} from WLC {}",
                    blockedMac.getClientMac(), blockedMac.getWlc().getId());
        } else {
            log.error("Failed to remove MAC address: {} from WLC {}",
                    blockedMac.getClientMac(), blockedMac.getWlc().getId());
        }

        return success;
    }

    /**
     * Get blocked MAC addresses from both WLCs via SSH
     */
    public List<BlockedMacTo> getBlockedMacAddresses() {
        try {
            log.info("Fetching blocked MAC addresses via SSH...");
            return sshBlackListUtil.getBlockedMacAddresses();

        } catch (Exception e) {
            log.error("Error fetching MAC addresses via SSH: {}", e.getMessage(), e);
            return List.of(); // Return empty list on error
        }
    }
}
