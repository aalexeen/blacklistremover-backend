package org.bhmc.blacklistremover.config;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bhmc.blacklistremover.model.Role;
import org.bhmc.blacklistremover.model.User;
import org.bhmc.blacklistremover.repository.UserRepository;
import org.bhmc.blacklistremover.web.AuthUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import java.util.Optional;

@Configuration
@EnableWebSecurity
@Slf4j
@AllArgsConstructor
public class SecurityConfig {
    public static final PasswordEncoder PASSWORD_ENCODER = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    private final UserRepository userRepository;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    @Bean
    PasswordEncoder passwordEncoder() {
        return PASSWORD_ENCODER;
    }

    @Bean
    UserDetailsService userDetailsService() {
        return email -> {
            log.debug("Authenticating '{}'", email);
            Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(email);
            return new AuthUser(optionalUser.orElseThrow(
                    () -> new UsernameNotFoundException("User '" + email + "' was not found")));
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(request -> {
                var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                corsConfig.setAllowedOriginPatterns(List.of(
                        "http://localhost:*",        // Any port on localhost
                        "http://127.0.0.1:*",       // Loopback
                        "http://10.*.*.*:*",         // 10.x.x.x network
                        "http://172.16.*.*:*",       // 172.16.x.x network
                        "http://192.168.*.*:*"       // 192.168.x.x network
                ));
                corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")); // Allowed methods
                corsConfig.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept")); // Allowed headers
                corsConfig.setAllowCredentials(true); // Important: Allow credentials
                corsConfig.setMaxAge(3600L); // Cache preflight for 1 hour
                return corsConfig;
            }))
            .authorizeHttpRequests(ac ->
                    ac.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // Allow all OPTIONS requests
                      .requestMatchers("/favicon.ico").permitAll()
                      .requestMatchers("/", "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
                      .permitAll()
                      .requestMatchers("/api/auth/login").authenticated()
                      .requestMatchers("/api/auth/check").authenticated()
                      .requestMatchers("/api/auth/logout").authenticated()
                      .requestMatchers("/api/auth/profile").authenticated()
                      .requestMatchers("/api/blocked_macs").authenticated()
                      .requestMatchers("/api/blocked_macs/**").authenticated()
                      .requestMatchers("/api/admin/**").hasRole(Role.ADMIN.name())
                      .requestMatchers(HttpMethod.POST, "/api/profile").anonymous()
                      //.requestMatchers("/", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                      .requestMatchers("/api/**").authenticated())
            .httpBasic(hbc -> hbc.authenticationEntryPoint(authenticationEntryPoint))
            .sessionManagement(smc -> smc.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }
}