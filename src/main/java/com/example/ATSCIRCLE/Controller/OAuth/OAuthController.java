package com.example.ATSCIRCLE.Controller.OAuth;

import com.example.ATSCIRCLE.Models.Users;
import com.example.ATSCIRCLE.Repository.UserRepository;
import com.example.ATSCIRCLE.Service.OAuth.GoogleOAuthService;
import com.example.ATSCIRCLE.Service.OAuth.MicrosoftOAuthService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/oauth2")
public class OAuthController {

    @Autowired
    private GoogleOAuthService googleOAuthService;

    @Autowired
    private MicrosoftOAuthService microsoftOAuthService;

    @Autowired
    private UserRepository userRepository;

    // ─────────────────────────────────────────
    // GOOGLE
    // ─────────────────────────────────────────

    // GET /api/oauth2/google/url?userId=abc123
    @GetMapping("/google/url")
    public ResponseEntity<Map<String, String>> getGoogleAuthUrl(@RequestParam String userId) {
        String url = googleOAuthService.generateAuthUrl(userId);
        return ResponseEntity.ok(Map.of("url", url));
    }

    // Google calls this after user approves
    // GET /api/oauth2/callback/google?code=...&state=userId
    @GetMapping("/callback/google")
    public RedirectView handleGoogleCallback(
            @RequestParam String code,
            @RequestParam String state) {
        try {
            Map<String, Object> tokens = googleOAuthService.exchangeCodeForTokens(code);

            String accessToken  = (String) tokens.get("access_token");
            String refreshToken = (String) tokens.get("refresh_token");
            Integer expiresIn   = (Integer) tokens.get("expires_in");

            String googleEmail = googleOAuthService.getGoogleUserEmail(accessToken);

            Users user = userRepository.findById(state).orElseThrow();
            user.setGoogleAccessToken(accessToken);
            user.setGoogleRefreshToken(refreshToken);
            user.setGoogleTokenExpiry(LocalDateTime.now().plusSeconds(expiresIn));
            user.setGoogleEmail(googleEmail);
            user.setGoogleConnected(true);

            // ఒక్కటే connected అయితే auto default
            if (!user.isMicrosoftConnected()) {
                user.setDefaultEmailProvider(Users.EmailProvider.GOOGLE);
            }

            userRepository.save(user);

            return new RedirectView("http://localhost:3000/admin/profile?google=connected");

        } catch (Exception e) {
            e.printStackTrace();
            return new RedirectView("http://localhost:3000/admin/profile?google=failed");
        }
    }

    // GET /api/oauth2/google/status?userId=abc123
    @GetMapping("/google/status")
    public ResponseEntity<Map<String, Object>> getGoogleStatus(@RequestParam String userId) {
        Users user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(Map.of(
            "connected", user.isGoogleConnected(),
            "googleEmail", user.getGoogleEmail() != null ? user.getGoogleEmail() : ""
        ));
    }

    // DELETE /api/oauth2/google/disconnect?userId=abc123
    @DeleteMapping("/google/disconnect")
    public ResponseEntity<Map<String, String>> disconnectGoogle(@RequestParam String userId) {
        Users user = userRepository.findById(userId).orElseThrow();

        user.setGoogleAccessToken(null);
        user.setGoogleRefreshToken(null);
        user.setGoogleTokenExpiry(null);
        user.setGoogleEmail(null);
        user.setGoogleConnected(false);

        // Default auto switch
        if (user.getDefaultEmailProvider() == Users.EmailProvider.GOOGLE) {
            if (user.isMicrosoftConnected()) {
                user.setDefaultEmailProvider(Users.EmailProvider.MICROSOFT);
            } else {
                user.setDefaultEmailProvider(Users.EmailProvider.NONE);
            }
        }

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Google disconnected successfully"));
    }

    // ─────────────────────────────────────────
    // MICROSOFT
    // ─────────────────────────────────────────

    // GET /api/oauth2/microsoft/url?userId=abc123
    @GetMapping("/microsoft/url")
    public ResponseEntity<Map<String, String>> getMicrosoftAuthUrl(@RequestParam String userId) {
        String url = microsoftOAuthService.generateAuthUrl(userId);
        return ResponseEntity.ok(Map.of("url", url));
    }

    // Microsoft calls this after user approves
    // GET /api/oauth2/callback/microsoft?code=...&state=userId
   @GetMapping("/callback/microsoft")
public RedirectView handleMicrosoftCallback(
        @RequestParam(required = false) String code,
        @RequestParam(required = false) String state,
        @RequestParam(required = false) String error,
        @RequestParam(value = "error_description", required = false) String errorDescription,
        HttpServletRequest request) {  // Add this

    // DEBUG - print everything Microsoft sent
    System.out.println("=== MICROSOFT CALLBACK HIT ===");
    System.out.println("Full URL: " + request.getRequestURL() + "?" + request.getQueryString());
    System.out.println("code: " + code);
    System.out.println("state: " + state);
    System.out.println("error: " + error);
    System.out.println("error_description: " + errorDescription);

    if (error != null) {
        System.err.println("OAuth Error: " + error + " - " + errorDescription);
        return new RedirectView("http://localhost:3000/admin/profile?microsoft=failed&reason=" + error);
    }

    if (code == null) {
        return new RedirectView("http://localhost:3000/admin/profile?microsoft=failed&reason=no_code");
    }
        try {
            Map<String, Object> tokens = microsoftOAuthService.exchangeCodeForTokens(code);

            String accessToken  = (String) tokens.get("access_token");
            String refreshToken = (String) tokens.get("refresh_token");
            Integer expiresIn   = (Integer) tokens.get("expires_in");

            String microsoftEmail = microsoftOAuthService.getMicrosoftUserEmail(accessToken);

            Users user = userRepository.findById(state).orElseThrow();
            user.setMicrosoftAccessToken(accessToken);
            user.setMicrosoftRefreshToken(refreshToken);
            user.setMicrosoftTokenExpiry(LocalDateTime.now().plusSeconds(expiresIn));
            user.setMicrosoftEmail(microsoftEmail);
            user.setMicrosoftConnected(true);

            // ఒక్కటే connected అయితే auto default
            if (!user.isGoogleConnected()) {
                user.setDefaultEmailProvider(Users.EmailProvider.MICROSOFT);
            }

            userRepository.save(user);

            return new RedirectView("http://localhost:3000/admin/profile?microsoft=connected");

        } catch (Exception e) {
            e.printStackTrace();
            return new RedirectView("http://localhost:3000/admin/profile?microsoft=failed");
        }
    }

    // GET /api/oauth2/microsoft/status?userId=abc123
    @GetMapping("/microsoft/status")
    public ResponseEntity<Map<String, Object>> getMicrosoftStatus(@RequestParam String userId) {
        Users user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(Map.of(
            "connected", user.isMicrosoftConnected(),
            "microsoftEmail", user.getMicrosoftEmail() != null ? user.getMicrosoftEmail() : ""
        ));
    }

    // DELETE /api/oauth2/microsoft/disconnect?userId=abc123
    @DeleteMapping("/microsoft/disconnect")
    public ResponseEntity<Map<String, String>> disconnectMicrosoft(@RequestParam String userId) {
        Users user = userRepository.findById(userId).orElseThrow();

        user.setMicrosoftAccessToken(null);
        user.setMicrosoftRefreshToken(null);
        user.setMicrosoftTokenExpiry(null);
        user.setMicrosoftEmail(null);
        user.setMicrosoftConnected(false);

        // Default auto switch
        if (user.getDefaultEmailProvider() == Users.EmailProvider.MICROSOFT) {
            if (user.isGoogleConnected()) {
                user.setDefaultEmailProvider(Users.EmailProvider.GOOGLE);
            } else {
                user.setDefaultEmailProvider(Users.EmailProvider.NONE);
            }
        }

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Microsoft disconnected successfully"));
    }

    // ─────────────────────────────────────────
    // DEFAULT PROVIDER
    // ─────────────────────────────────────────
    // PUT /api/oauth2/default?userId=abc123&provider=GOOGLE
    @PutMapping("/default")
    public ResponseEntity<Map<String, String>> setDefaultProvider(
            @RequestParam String userId,
            @RequestParam Users.EmailProvider provider) {

        Users user = userRepository.findById(userId).orElseThrow();

        // Validation — connect అయినదే default పెట్టాలి
        if (provider == Users.EmailProvider.GOOGLE && !user.isGoogleConnected()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Google is not connected"));
        }
        if (provider == Users.EmailProvider.MICROSOFT && !user.isMicrosoftConnected()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Microsoft is not connected"));
        }

        user.setDefaultEmailProvider(provider);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Default provider set to " + provider));
    }

    // GET /api/oauth2/default?userId=abc123
    @GetMapping("/default")
    public ResponseEntity<Map<String, Object>> getDefaultProvider(@RequestParam String userId) {
        Users user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(Map.of(
            "defaultProvider", user.getDefaultEmailProvider(),
            "googleConnected", user.isGoogleConnected(),
            "microsoftConnected", user.isMicrosoftConnected(),
            "googleEmail", user.getGoogleEmail() != null ? user.getGoogleEmail() : "",
            "microsoftEmail", user.getMicrosoftEmail() != null ? user.getMicrosoftEmail() : ""
        ));
    }
}