package org.bhmc.blacklistremover;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@PropertySource("classpath:config.properties")
public class BlacklistRemoverApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlacklistRemoverApplication.class, args);
    }
}
