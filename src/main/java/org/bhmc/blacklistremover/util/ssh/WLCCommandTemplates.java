package org.bhmc.blacklistremover.util.ssh;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SSH command templates for different WLC vendors
 */
@Component
@Slf4j
public class WLCCommandTemplates {

    // Aruba WLC Commands
    public static class Aruba {
        public static final String SHOW_BLACKLIST = "show ap blacklist-clients";
        public static final String SHOW_BLACKLIST_DETAIL = "show ap blacklist-clients detail";
        public static final String SHOW_ACCESS_CONTROL = "show ap access-control-list";

        public static final String REMOVE_MAC_TEMPLATE = """
            configure terminal
            ap access-control-list
            no deny-mac %s
            exit
            exit
            write memory
            """;

        public static final String ADD_MAC_TEMPLATE = """
            configure terminal
            ap access-control-list
            deny-mac %s
            exit
            exit
            write memory
            """;
    }

    // Cisco WLC Commands (if needed)
    public static class Cisco {
        public static final String SHOW_BLACKLIST = "show exclusionlist";
        public static final String REMOVE_MAC_TEMPLATE = "config exclusionlist delete %s";
        public static final String ADD_MAC_TEMPLATE = "config exclusionlist add %s";
    }

    // Add other vendor commands as needed
    public static class Generic {
        public static final String SHOW_VERSION = "show version";
        public static final String SHOW_USERS = "show user-table";
        public static final String EXIT = "exit";
    }
}