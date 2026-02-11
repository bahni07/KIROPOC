package com.example.userregistration.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordHashingService {

    // BCrypt with cost factor 12 (as per requirements)
    private static final int BCRYPT_COST_FACTOR = 12;
    private final BCryptPasswordEncoder passwordEncoder;

    public PasswordHashingService() {
        this.passwordEncoder = new BCryptPasswordEncoder(BCRYPT_COST_FACTOR);
    }

    /**
     * Hash a plain text password using BCrypt with cost factor 12
     * 
     * @param plainPassword the plain text password to hash
     * @return the BCrypt hashed password
     */
    public String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return passwordEncoder.encode(plainPassword);
    }

    /**
     * Verify a plain text password against a BCrypt hash
     * 
     * @param plainPassword the plain text password to verify
     * @param hashedPassword the BCrypt hash to verify against
     * @return true if the password matches the hash, false otherwise
     */
    public boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        return passwordEncoder.matches(plainPassword, hashedPassword);
    }
}
