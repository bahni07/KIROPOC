package com.example.userregistration.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encryption service for OAuth tokens using AES-256-GCM
 * Provides authenticated encryption with associated data (AEAD)
 */
@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // 128 bits
    private static final int GCM_IV_LENGTH = 12; // 12 bytes (96 bits) recommended for GCM
    private static final int AES_KEY_SIZE = 256; // 256 bits
    
    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public EncryptionService(@Value("${app.encryption.key}") String base64Key) {
        this.secureRandom = new SecureRandom();
        
        // Decode the base64-encoded key
        byte[] decodedKey = Base64.getDecoder().decode(base64Key);
        
        // Validate key length (must be 32 bytes for AES-256)
        if (decodedKey.length != 32) {
            throw new IllegalArgumentException(
                "Encryption key must be 32 bytes (256 bits) for AES-256. Provided: " + decodedKey.length + " bytes");
        }
        
        this.secretKey = new SecretKeySpec(decodedKey, "AES");
    }

    /**
     * Encrypt a token using AES-256-GCM
     * 
     * @param plainToken the plain text token to encrypt
     * @return Base64-encoded encrypted token with IV prepended
     */
    public String encryptToken(String plainToken) {
        if (plainToken == null || plainToken.isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        
        try {
            // Generate random IV (Initialization Vector)
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            // Initialize cipher for encryption
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
            
            // Encrypt the token
            byte[] plainBytes = plainToken.getBytes(StandardCharsets.UTF_8);
            byte[] cipherText = cipher.doFinal(plainBytes);
            
            // Combine IV and ciphertext: [IV][ciphertext]
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);
            
            // Return Base64-encoded result
            return Base64.getEncoder().encodeToString(byteBuffer.array());
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt token: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypt a token using AES-256-GCM
     * 
     * @param encryptedToken Base64-encoded encrypted token with IV prepended
     * @return the decrypted plain text token
     */
    public String decryptToken(String encryptedToken) {
        if (encryptedToken == null || encryptedToken.isEmpty()) {
            throw new IllegalArgumentException("Encrypted token cannot be null or empty");
        }
        
        try {
            // Decode Base64
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedToken);
            
            // Extract IV and ciphertext
            ByteBuffer byteBuffer = ByteBuffer.wrap(decodedBytes);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);
            
            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
            
            // Decrypt the token
            byte[] plainBytes = cipher.doFinal(cipherText);
            
            return new String(plainBytes, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt token: " + e.getMessage(), e);
        }
    }

    /**
     * Generate a new AES-256 key (for key generation utility)
     * This method is provided for convenience to generate encryption keys
     * 
     * @return Base64-encoded AES-256 key
     */
    public static String generateNewKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(AES_KEY_SIZE);
            SecretKey key = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate key: " + e.getMessage(), e);
        }
    }
}
