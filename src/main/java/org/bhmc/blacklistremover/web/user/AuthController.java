package org.bhmc.blacklistremover.web.user;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.bhmc.blacklistremover.model.User;
import org.bhmc.blacklistremover.to.LoginResponseTo;
import org.bhmc.blacklistremover.web.AuthUser;

import java.util.Map;

@RestController
@RequestMapping(value = AuthController.REST_URL, produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController extends AbstractUserController {

    static final String REST_URL = "/api/auth";

    @PostMapping("/login")
    public ResponseEntity<LoginResponseTo> login(@AuthenticationPrincipal AuthUser authUser) {
        log.info("login user {}", authUser.getUser().getEmail());
        // If this method is reached, authentication was successful
        User user = authUser.getUser();

        LoginResponseTo response = new LoginResponseTo(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRoles()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/check")
    public ResponseEntity<LoginResponseTo> checkAuth(@AuthenticationPrincipal AuthUser authUser) {
        log.info("check auth for user {}", authUser.getUser().getEmail());
        // For checking if current session/credentials are still valid
        User user = authUser.getUser();

        LoginResponseTo response = new LoginResponseTo(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRoles()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    public User getProfile(@AuthenticationPrincipal AuthUser authUser) {
        log.info("get profile for user {}", authUser.getUser().getEmail());
        return authUser.getUser();
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@AuthenticationPrincipal AuthUser authUser) {
        log.info("logout user {}", authUser.getUser().getEmail());
        // With HTTP Basic and stateless sessions, logout is primarily client-side
        // This endpoint mainly serves for logging and potential future server-side logout logic
        Map<String, String> response = Map.of(
                "message", "Logout successful",
                "timestamp", java.time.LocalDateTime.now().toString()
        );
        return ResponseEntity.ok(response);
    }
}
