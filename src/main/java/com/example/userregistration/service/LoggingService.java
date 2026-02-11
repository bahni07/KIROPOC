package com.example.userregistration.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Logging service that sanitizes sensitive data before logging
 * Ensures passwords, tokens, and other sensitive information are never logged
 */
@Service
public class LoggingService {

    private static final Logger logger = LoggerFactory.getLogger(LoggingService.class);
    
    // Patterns to detect and sanitize sensitive data
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "(password|pwd|pass)[\"']?\\s*[:=]\\s*[\"']?([^\"',\\s}]+)", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
        "(token|accessToken|refreshToken|verificationToken|oauthToken)[\"']?\\s*[:=]\\s*[\"']?([^\"',\\s}]+)", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern API_KEY_PATTERN = Pattern.compile(
        "(apiKey|api_key|secret|clientSecret)[\"']?\\s*[:=]\\s*[\"']?([^\"',\\s}]+)", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final String REDACTED = "[REDACTED]";

    /**
     * Sanitize a log message by removing sensitive data
     * 
     * @param message the message to sanitize
     * @return sanitized message with sensitive data replaced
     */
    public String sanitizeLogMessage(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        
        String sanitized = message;
        
        // Sanitize passwords
        sanitized = PASSWORD_PATTERN.matcher(sanitized).replaceAll("$1=" + REDACTED);
        
        // Sanitize tokens
        sanitized = TOKEN_PATTERN.matcher(sanitized).replaceAll("$1=" + REDACTED);
        
        // Sanitize API keys and secrets
        sanitized = API_KEY_PATTERN.matcher(sanitized).replaceAll("$1=" + REDACTED);
        
        return sanitized;
    }

    /**
     * Log registration event (email registration)
     */
    public void logEmailRegistration(String email, boolean success) {
        String sanitizedEmail = maskEmail(email);
        if (success) {
            logger.info("Email registration successful for user: {}", sanitizedEmail);
        } else {
            logger.warn("Email registration failed for user: {}", sanitizedEmail);
        }
    }

    /**
     * Log OAuth registration event
     */
    public void logOAuthRegistration(String email, String provider, boolean success) {
        String sanitizedEmail = maskEmail(email);
        if (success) {
            logger.info("OAuth registration successful for user: {} via provider: {}", 
                sanitizedEmail, provider);
        } else {
            logger.warn("OAuth registration failed for user: {} via provider: {}", 
                sanitizedEmail, provider);
        }
    }

    /**
     * Log email verification event
     */
    public void logEmailVerification(String email, boolean success) {
        String sanitizedEmail = maskEmail(email);
        if (success) {
            logger.info("Email verification successful for user: {}", sanitizedEmail);
        } else {
            logger.warn("Email verification failed for user: {}", sanitizedEmail);
        }
    }

    /**
     * Log validation error
     */
    public void logValidationError(String email, String errorMessage) {
        String sanitizedEmail = maskEmail(email);
        String sanitizedMessage = sanitizeLogMessage(errorMessage);
        logger.warn("Validation error for user: {} - {}", sanitizedEmail, sanitizedMessage);
    }

    /**
     * Log authentication attempt
     */
    public void logAuthenticationAttempt(String email, boolean success) {
        String sanitizedEmail = maskEmail(email);
        if (success) {
            logger.info("Authentication successful for user: {}", sanitizedEmail);
        } else {
            logger.warn("Authentication failed for user: {}", sanitizedEmail);
        }
    }

    /**
     * Log security event
     */
    public void logSecurityEvent(String eventType, String details) {
        String sanitizedDetails = sanitizeLogMessage(details);
        logger.warn("Security event - Type: {} - Details: {}", eventType, sanitizedDetails);
    }

    /**
     * Log error with sanitization
     */
    public void logError(String message, Throwable throwable) {
        String sanitizedMessage = sanitizeLogMessage(message);
        logger.error(sanitizedMessage, throwable);
    }

    /**
     * Log info with sanitization
     */
    public void logInfo(String message) {
        String sanitizedMessage = sanitizeLogMessage(message);
        logger.info(sanitizedMessage);
    }

    /**
     * Log debug with sanitization
     */
    public void logDebug(String message) {
        String sanitizedMessage = sanitizeLogMessage(message);
        logger.debug(sanitizedMessage);
    }

    /**
     * Mask email address for logging (show first 2 chars and domain)
     * Example: john.doe@example.com -> jo***@example.com
     */
    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "[UNKNOWN]";
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "[INVALID_EMAIL]";
        }
        
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        if (localPart.length() <= 2) {
            return "**" + domain;
        }
        
        return localPart.substring(0, 2) + "***" + domain;
    }
}
