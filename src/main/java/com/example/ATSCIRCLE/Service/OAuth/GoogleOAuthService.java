package com.example.ATSCIRCLE.Service.OAuth;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class GoogleOAuthService {

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.redirect.uri}")
    private String redirectUri;

    private static final List<String> SCOPES = Arrays.asList(
        "https://www.googleapis.com/auth/gmail.send",
        "https://www.googleapis.com/auth/calendar",
        "https://www.googleapis.com/auth/userinfo.email",
        "https://www.googleapis.com/auth/userinfo.profile"
    );

    // Generate Google login URL
    public String generateAuthUrl(String userId) {
        String scope = String.join(" ", SCOPES);

        return "https://accounts.google.com/o/oauth2/v2/auth?"
                + "client_id=" + clientId
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8)
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state=" + userId;
    }

    // Exchange code for tokens
    // FIX: Added proper response validation and try-with-resources to close response body
    public Map<String, Object> exchangeCodeForTokens(String code) throws Exception {
        OkHttpClient client = new OkHttpClient();

        RequestBody body = new FormBody.Builder()
                .add("code", code)
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("redirect_uri", redirectUri)
                .add("grant_type", "authorization_code")
                .build();

        Request request = new Request.Builder()
                .url("https://oauth2.googleapis.com/token")
                .post(body)
                .build();

        // FIX: Use try-with-resources to avoid resource leaks
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            // FIX: Validate HTTP response before parsing
            if (!response.isSuccessful()) {
                throw new RuntimeException("Token exchange failed: HTTP " + response.code() + " - " + responseBody);
            }

            if (responseBody.isEmpty()) {
                throw new RuntimeException("Token exchange returned empty response body");
            }

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(responseBody, Map.class);
        }
    }

    // Get user email from Google
    // FIX: Added proper response validation and try-with-resources
    public String getGoogleUserEmail(String accessToken) throws Exception {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://www.googleapis.com/oauth2/v2/userinfo")
                .header("Authorization", "Bearer " + accessToken)
                .build();

        // FIX: Use try-with-resources to avoid resource leaks
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            // FIX: Validate HTTP response before parsing
            if (!response.isSuccessful()) {
                throw new RuntimeException("Failed to fetch user info: HTTP " + response.code() + " - " + responseBody);
            }

            if (responseBody.isEmpty()) {
                throw new RuntimeException("User info response was empty");
            }

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> userInfo = mapper.readValue(responseBody, Map.class);

            String email = (String) userInfo.get("email");

            // FIX: Validate email is not null before returning
            if (email == null || email.isEmpty()) {
                throw new RuntimeException("Google did not return an email address");
            }

            return email;
        }
    }
}