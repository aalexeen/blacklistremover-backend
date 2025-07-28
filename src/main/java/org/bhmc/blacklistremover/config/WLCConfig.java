package org.bhmc.blacklistremover.config;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WLCConfig {
    private int id;
    private String host;
    private int port;
    private String username;
    private String password;
    private String enapassword;
}
