package com.example.userregistration.service;

import com.example.userregistration.dto.OAuthAuthorizationResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class OAuth2Service {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    // Google OAuth2 Configuration
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;
    
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;
    
    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String googleRedirectUri;
    
    // Amazon OAuth2 Configuration
    @Value("${spring.security.oauth2.client.registration.amazon.client-id}")
    private String amazonClientId;
    
    @Value("${spring.security.oauth2.client.registration.amazon.client-secret}")
    private String amazonClientSecret;
    
    @Value("${spring.security.oauth2.client.registration.amazon.redirect-uri}")
    private String amazonRedirectUri;
    
    @Value("${app.base-url}")
    private String baseUrl;
    
    // OAuth2 endpoints
    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
    
    private static final String AMAZON_AUTH_URL = "https://www.amazon.com/ap/oa";
    private static final String AMAZON_TOKEN_URL = "https://api.amazon.com/auth/o2/token";
    private static final String AMAZON_USERINFO_URL = "https://api.amazon.com/user/profile";
    
    // Store PKCE verifiers temporarily (in production, use Redis or session storage)
    private final Map<String, String> pkceVerifiers = new HashMap<>();

    public OAuth2Service() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generate authorization URL for Google OAuth2 with PKCE
     */
    public OAuthAuthorizationResponse getGoogleAuthorizationUrl(String state) {
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);
        
        // Store verifier for later use
        pkceVerifiers.put(state, codeVerifier);
        
        String authUrl = UriComponentsBuilder.fromHttpUrl(GOOGLE_AUTH_URL)
                .queryParam("client_id", googleClientId)
                .queryParam("redirect_uri", googleRedirectUri.replace("{baseUrl}", baseUrl))
                .queryParam("response_type", "code")
                .queryParam("scope", "openid profile email")
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .build()
                .toUriString();
        
        return new OAuthAuthorizationResponse(authUrl, state);
    }

    /**
     * Generate authorization URL for Amazon OAuth2 with PKCE
     */
    public OAuthAuthorizationResponse getAmazonAuthorizationUrl(String state) {
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);
        
        // Store verifier for later use
        pkceVerifiers.put(state, codeVerifier);
        
        String authUrl = UriComponentsBuilder.fromHttpUrl(AMAZON_AUTH_URL)
                .queryParam("client_id", amazonClientId)
                .queryParam("redirect_uri", amazonRedirectUri.replace("{baseUrl}", baseUrl))
                .queryParam("response_type", "code")
                .queryParam("scope", "profile")
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .build()
                .toUriString();
        
        return new OAuthAuthorizationResponse(authUrl, state);
    }

    /**
     * Exchange authorization code for access token (Google)
     */
    public String exchangeGoogleCodeForToken(String code, String state) {
        String codeVerifier = pkceVerifiers.remove(state);
        if (codeVerifier == null) {
            throw new IllegalStateException("Invalid state parameter or PKCE verifier not found");
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("redirect_uri", googleRedirectUri.replace("{baseUrl}", baseUrl));
        params.add("grant_type", "authorization_code");
        params.add("code_verifier", codeVerifier);
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                GOOGLE_TOKEN_URL, request, String.class);
            
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            return jsonNode.get("access_token").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to exchange code for token: " + e.getMessage(), e);
        }
    }

    /**
     * Exchange authorization code for access token (Amazon)
     */
    public String exchangeAmazonCodeForToken(String code, String state) {
        String codeVerifier = pkceVerifiers.remove(state);
        if (codeVerifier == null) {
            throw new IllegalStateException("Invalid state parameter or PKCE verifier not found");
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", amazonClientId);
        params.add("client_secret", amazonClientSecret);
        params.add("redirect_uri", amazonRedirectUri.replace("{baseUrl}", baseUrl));
        params.add("grant_type", "authorization_code");
        params.add("code_verifier", codeVerifier);
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                AMAZON_TOKEN_URL, request, String.class);
            
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            return jsonNode.get("access_token").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to exchange code for token: " + e.getMessage(), e);
        }
    }

    /**
     * Get user info from Google using access token
     */
    public Map<String, String> getGoogleUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                GOOGLE_USERINFO_URL, HttpMethod.GET, entity, String.class);
            
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            
            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("email", jsonNode.get("email").asText());
            userInfo.put("firstName", jsonNode.has("given_name") ? 
                jsonNode.get("given_name").asText() : "");
            userInfo.put("lastName", jsonNode.has("family_name") ? 
                jsonNode.get("family_name").asText() : "");
            userInfo.put("providerId", jsonNode.get("id").asText());
            userInfo.put("provider", "GOOGLE");
            
            return userInfo;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get user info: " + e.getMessage(), e);
        }
    }

    /**
     * Get user info from Amazon using access token
     */
    public Map<String, String> getAmazonUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                AMAZON_USERINFO_URL, HttpMethod.GET, entity, String.class);
            
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            
            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("email", jsonNode.get("email").asText());
            userInfo.put("firstName", jsonNode.has("name") ? 
                jsonNode.get("name").asText().split(" ")[0] : "");
            userInfo.put("lastName", jsonNode.has("name") && 
                jsonNode.get("name").asText().split(" ").length > 1 ? 
                jsonNode.get("name").asText().split(" ")[1] : "");
            userInfo.put("providerId", jsonNode.get("user_id").asText());
            userInfo.put("provider", "AMAZON");
            
            return userInfo;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get user info: " + e.getMessage(), e);
        }
    }

    /**
     * Generate PKCE code verifier (43-128 characters)
     */
    private String generateCodeVerifier() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] codeVerifier = new byte[32];
        secureRandom.nextBytes(codeVerifier);
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(codeVerifier);
    }

    /**
     * Generate PKCE code challenge from verifier using SHA-256
     */
    private String generateCodeChallenge(String codeVerifier) {
        try {
            byte[] bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(bytes);
            return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
