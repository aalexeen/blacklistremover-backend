package org.bhmc.blacklistremover.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Enhanced configuration class for WLC SSH connections
 */
@Component
@ConfigurationProperties(prefix = "wlc")
@Data
public class WLCConfiguration {

    private SSH ssh = new SSH();
    private Commands commands = new Commands();

    @Data
    public static class SSH {
        private WLCConnection wlc1 = new WLCConnection();
        private WLCConnection wlc2 = new WLCConnection();
        private int connectionTimeout = 30;
        private int commandTimeout = 15;
        private int healthCheckInterval = 30;
    }

    @Data
    public static class WLCConnection {
        private String host;
        private int port = 22;
        private String username;
        private String password;
        private String enapassword;
        private String keyFile; // For key-based auth
        private String vendor = "aruba"; // aruba, cisco, etc.
    }

    @Data
    public static class Commands {
        private String showBlacklist = "show ap blacklist-clients";
        private String removeMacTemplate = "stm remove-blacklist-client %s";
    }
}