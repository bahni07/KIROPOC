package com.example.userregistration.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class VerificationTokenService {

    private static final int TOKEN_BYTE_LENGTH = 32;
    private static final int TOKEN_EXPIRATION_HOURS = 24;
    private final SecureRandom secureRandom;

    public VerificationTokenService() {
        this.secureRandom = new SecureRandom();
    }

    /**
     * Generate a cryptographically secure random token (32 bytes, Base64 encoded)
     * 
     * @return the generated token as a Base64 string
     */
    public String generateToken() {
        byte[] tokenBytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Hash a token using SHA-256
     * 
     * @param token the token to hash
     * @return the SHA-256 hash as a hex string
     */
    public String hashToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Check if a token has expired (24-hour expiration)
     * 
     * @param createdAt the timestamp when the token was created
     * @return true if the token has expired, false otherwise
     */
    public boolean isTokenExpired(LocalDateTime createdAt) {
        if (createdAt == null) {
            return true;
        }
        LocalDateTime expirationTime = createdAt.plusHours(TOKEN_EXPIRATION_HOURS);
        return LocalDateTime.now().isAfter(expirationTime);
    }

    /**
     * Convert byte array to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
