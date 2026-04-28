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
public class MicrosoftOAuthService {

    @Value("${microsoft.client.id}")
    private String clientId;

    @Value("${microsoft.client.secret}")
    private String clientSecret;

    @Value("${microsoft.redirect.uri}")
    private String redirectUri;

    private static final List<String> SCOPES = Arrays.asList(
    "openid",
    "profile",
    "offline_access",
    "https://graph.microsoft.com/User.Read",
    "https://graph.microsoft.com/Mail.Send",
    "https://graph.microsoft.com/Calendars.ReadWrite"
);

    // Generate Microsoft login URL
    public String generateAuthUrl(String userId) {
    String scope = String.join(" ", SCOPES);

    return "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?"
            + "client_id=" + clientId
            + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
            + "&response_type=code"
            + "&response_mode=query"
            + "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8)
            + "&prompt=select_account"  // prompt=consent 
            + "&state=" + userId;
}

    // Exchange code for tokens
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
                .url("https://login.microsoftonline.com/common/oauth2/v2.0/token")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

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

    // Get user email from Microsoft Graph
    public String getMicrosoftUserEmail(String accessToken) throws Exception {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://graph.microsoft.com/v1.0/me")
                .header("Authorization", "Bearer " + accessToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                throw new RuntimeException("Failed to fetch user info: HTTP " + response.code() + " - " + responseBody);
            }

            if (responseBody.isEmpty()) {
                throw new RuntimeException("User info response was empty");
            }

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> userInfo = mapper.readValue(responseBody, Map.class);

            // Microsoft returns "mail" or "userPrincipalName"
            String email = (String) userInfo.get("mail");
            if (email == null || email.isEmpty()) {
                email = (String) userInfo.get("userPrincipalName");
            }

            if (email == null || email.isEmpty()) {
                throw new RuntimeException("Microsoft did not return an email address");
            }

            return email;
        }
    }
} 
