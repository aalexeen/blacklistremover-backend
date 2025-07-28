package org.bhmc.blacklistremover.service.ssh;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.bhmc.blacklistremover.model.BlockedMac;
import org.bhmc.blacklistremover.service.BlockedMacSyncService;
import org.bhmc.blacklistremover.to.BlockedMacTo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;


@Component
@Slf4j
public class SSHBlackListUtil {

    @Autowired
    private SSHConnectionManager connectionManager;

    @Autowired
    private BlockedMacSyncService syncService;

    private volatile List<BlockedMacTo> cachedMacAddresses = new ArrayList<>();
    private volatile Instant lastUpdateTime = Instant.EPOCH;
    private final ReentrantLock cacheLock = new ReentrantLock();

    // Commands for different WLC types (Aruba, Cisco, etc.)
    private static final String ARUBA_SHOW_BLACKLIST = "show ap blacklist-clients";
    private static final String ARUBA_REMOVE_MAC = "stm remove-blacklist-client %s";

    @Scheduled(fixedDelay = 600000) // Every 5 minutes
    @Transactional
    public void scheduledUpdate() {
        log.info("Starting scheduled update and DB sync...");

        // Get fresh MAC addresses from WLCs
        List<BlockedMacTo> freshMacAddresses = getBlockedMacAddresses();

        if (!freshMacAddresses.isEmpty()) {
            // Sync to database using the dedicated sync service
            syncService.syncMacsToDatabase(freshMacAddresses);
            log.info("Scheduled update completed successfully");
        }
    }


    public List<BlockedMacTo> getBlockedMacAddresses() {
        Instant now = Instant.now();

        // Check if cache is still valid (2 minutes)
        if (lastUpdateTime.plusSeconds(120).isAfter(now) && !cachedMacAddresses.isEmpty()) {
            return new ArrayList<>(cachedMacAddresses);
        }

        cacheLock.lock();
        try {
            // Double-check inside lock
            if (lastUpdateTime.plusSeconds(120).isAfter(Instant.now()) && !cachedMacAddresses.isEmpty()) {
                return new ArrayList<>(cachedMacAddresses);
            }

            log.info("Fetching blacklists from both WLCs...");
            List<BlockedMacTo> allMacs = new ArrayList<>();

            // Fetch from both WLCs concurrently
            CompletableFuture<List<BlockedMacTo>> future1 = CompletableFuture.supplyAsync(
                    () -> fetchBlacklistFromWLC(1)
            );

            CompletableFuture<List<BlockedMacTo>> future2 = CompletableFuture.supplyAsync(
                    () -> fetchBlacklistFromWLC(2)
            );

            try {
                List<BlockedMacTo> wlc1Macs = future1.get(30, TimeUnit.SECONDS);
                List<BlockedMacTo> wlc2Macs = future2.get(30, TimeUnit.SECONDS);

                allMacs.addAll(wlc1Macs);
                allMacs.addAll(wlc2Macs);

                cachedMacAddresses = allMacs;
                lastUpdateTime = Instant.now();

                log.info("Successfully fetched {} MAC addresses from WLCs", allMacs.size());
            } catch (Exception e) {
                log.error("Error fetching MAC addresses: {}", e.getMessage(), e);
                // Return cached data if available
                if (!cachedMacAddresses.isEmpty()) {
                    log.info("Returning cached MAC addresses due to fetch error");
                    return new ArrayList<>(cachedMacAddresses);
                }
            }

            return new ArrayList<>(allMacs);
        } finally {
            cacheLock.unlock();
        }
    }

    private List<BlockedMacTo> fetchBlacklistFromWLC(int wlcId) {
        List<BlockedMacTo> macAddresses = new ArrayList<>();

        try {
            String output = connectionManager.executeCommand(wlcId, ARUBA_SHOW_BLACKLIST, 10);
            macAddresses = parseBlacklistOutput(output, wlcId);

            log.info("Fetched {} MAC addresses from WLC {}", macAddresses.size(), wlcId);
        } catch (Exception e) {
            log.error("Error fetching blacklist from WLC {}: {}", wlcId, e.getMessage(), e);
        }

        return macAddresses;
    }

    private List<BlockedMacTo> parseBlacklistOutput(String output, int wlcId) {
        List<BlockedMacTo> macAddresses = new ArrayList<>();

        if (output == null || output.trim().isEmpty()) {
            return macAddresses;
        }

        log.debug("Parsing blacklist output from WLC {}: {}", wlcId, output);

        String[] lines = output.split("\n");
        boolean inDataSection = false;

        for (String line : lines) {
            line = line.trim();

            // Skip empty lines
            if (line.isEmpty()) {
                continue;
            }

            // Look for the header line to start parsing data
            if (line.startsWith("STA") && line.contains("reason") && line.contains("block-time")) {
                inDataSection = true;
                continue;
            }

            // Skip the separator line with dashes
            if (line.startsWith("---") && line.contains("------")) {
                continue;
            }

            // Skip section headers like "Blacklisted Clients"
            if (line.startsWith("Blacklisted") || line.contains("Clients")) {
                continue;
            }

            // Parse data lines only when we're in the data section
            if (inDataSection) {
                // Split by whitespace but preserve multiple spaces
                String[] parts = line.split("\\s+");

                if (parts.length >= 4) {
                    String macAddress = parts[0];
                    String reason = parts[1];
                    String blockTimeStr = parts[2];
                    String remainingTimeStr = parts[3];

                    // Validate MAC address format (basic check)
                    if (isValidMacAddress(macAddress)) {
                        try {
                            // Parse block time as long (seconds)
                            long blockTimeSeconds = Long.parseLong(blockTimeStr);

                            // Convert block time to human readable format
                            String blockTimeFormatted = formatSecondsToHumanReadable(blockTimeSeconds);

                            // Format remaining time if it's not "Permanent"
                            String remainingTimeFormatted = remainingTimeStr;
                            if (!"Permanent".equalsIgnoreCase(remainingTimeStr)) {
                                try {
                                    long remainingSeconds = Long.parseLong(remainingTimeStr);
                                    remainingTimeFormatted = formatSecondsToHumanReadable(remainingSeconds);
                                } catch (NumberFormatException e) {
                                    // Keep original format if can't parse
                                    log.debug("Could not parse remaining time '{}' for MAC {}", remainingTimeStr, macAddress);
                                }
                            }

                            // Create BlockedMacTo object with formatted times
                            BlockedMacTo blockedMac = new BlockedMacTo(
                                    macAddress,
                                    blockTimeFormatted,  // Formatted block time
                                    remainingTimeFormatted,  // Formatted remaining time
                                    reason,
                                    wlcId,
                                    0  // userId - default to 0
                            );

                            macAddresses.add(blockedMac);
                            log.debug("Parsed MAC: {} from WLC {} with reason: {} and block time: {}",
                                    macAddress, wlcId, reason, blockTimeFormatted);
                        } catch (NumberFormatException e) {
                            log.warn("Could not parse block time '{}' for MAC {} from WLC {}",
                                    blockTimeStr, macAddress, wlcId);

                            // Still add the MAC with original time format if parsing fails
                            BlockedMacTo blockedMac = new BlockedMacTo(
                                    macAddress,
                                    blockTimeStr,  // Keep original format
                                    remainingTimeStr,
                                    reason,
                                    wlcId,
                                    0
                            );
                            macAddresses.add(blockedMac);
                        }
                    } else {
                        log.debug("Skipping invalid MAC address format: '{}' from WLC {}", macAddress, wlcId);
                    }
                } else {
                    log.debug("Skipping line with insufficient data parts ({}): '{}' from WLC {}",
                            parts.length, line, wlcId);
                }
            }
        }

        log.info("Parsed {} MAC addresses from WLC {} blacklist output", macAddresses.size(), wlcId);
        return macAddresses;
    }

    private String formatSecondsToHumanReadable(long totalSeconds) {
        if (totalSeconds <= 0) {
            return "0 secs";
        }

        long days = totalSeconds / (24 * 3600);
        long remainingAfterDays = totalSeconds % (24 * 3600);

        long hours = remainingAfterDays / 3600;
        long remainingAfterHours = remainingAfterDays % 3600;

        long minutes = remainingAfterHours / 60;
        long seconds = remainingAfterHours % 60;

        StringBuilder result = new StringBuilder();

        if (days > 0) {
            result.append(days).append(days == 1 ? " day" : " days");
        }

        if (hours > 0) {
            if (result.length() > 0) result.append(" ");
            result.append(hours).append(hours == 1 ? " hr" : " hrs");
        }

        if (minutes > 0) {
            if (result.length() > 0) result.append(" ");
            result.append(minutes).append(minutes == 1 ? " min" : " mins");
        }

        if (seconds > 0) {
            if (result.length() > 0) result.append(" ");
            result.append(seconds).append(seconds == 1 ? " sec" : " secs");
        }

        return result.toString();
    }


    public boolean removeMacAddress(BlockedMac blockedMac) {
        try {
            String command = String.format(ARUBA_REMOVE_MAC, blockedMac.getClientMac());
            String output = connectionManager.executeCommand(blockedMac.getWlc().getId(), command, 15);

            // Check if command was successful (adjust based on WLC response)
            boolean success = !output.toLowerCase().contains("error") &&
                    !output.toLowerCase().contains("failed");

            if (success) {
                log.info("Successfully removed MAC address: {}", blockedMac.getClientMac());
                // Invalidate cache to force refresh
                cacheLock.lock();
                try {
                    lastUpdateTime = Instant.EPOCH;
                } finally {
                    cacheLock.unlock();
                }
            } else {
                log.error("Failed to remove MAC address: {}. Output: {}", blockedMac.getClientMac(), output);
            }

            return success;
        } catch (Exception e) {
            log.error("Error removing MAC address {}: {}", blockedMac.getClientMac(), e.getMessage(), e);
            return false;
        }
    }

    private boolean isValidMacAddress(String mac) {
        if (mac == null || mac.trim().isEmpty()) {
            return false;
        }

        // Check for standard MAC format: xx:xx:xx:xx:xx:xx
        return mac.matches("^([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}$");
    }

    /**
     * Invalidate the cache to force fresh fetch on next request
     * Used after MAC deletion to ensure consistency
     */
    public void invalidateCache() {
        cacheLock.lock();
        try {
            lastUpdateTime = Instant.EPOCH;
            log.info("Cache invalidated - next request will fetch fresh data from WLCs");
        } finally {
            cacheLock.unlock();
        }
    }
}
