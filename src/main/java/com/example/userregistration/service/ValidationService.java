package com.example.userregistration.service;

import com.example.userregistration.dto.EmailRegistrationRequest;
import com.example.userregistration.repository.CustomerIdentityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ValidationService {

    private final CustomerIdentityRepository customerIdentityRepository;

    // RFC 5322 simplified email pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    // Password complexity requirements
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?]");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d");

    /**
     * Validate email format (RFC 5322 compliance)
     */
    public ValidationResult validateEmail(String email) {
        List<String> errors = new ArrayList<>();

        if (email == null || email.trim().isEmpty()) {
            errors.add("Email is required");
            return new ValidationResult(false, errors);
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            errors.add("Email format is invalid. Please provide a valid email address");
            return new ValidationResult(false, errors);
        }

        return new ValidationResult(true, errors);
    }

    /**
     * Validate password complexity requirements
     */
    public ValidationResult validatePassword(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isEmpty()) {
            errors.add("Password is required");
            return new ValidationResult(false, errors);
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            errors.add("Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }

        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            errors.add("Password must contain at least one special character (!@#$%^&*()_+-=[]{}|;:,.<>?)");
        }

        if (!NUMBER_PATTERN.matcher(password).find()) {
            errors.add("Password must contain at least one number (0-9)");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Check for duplicate email (case-insensitive)
     */
    public boolean checkDuplicateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return customerIdentityRepository.existsByEmailIgnoreCase(email);
    }

    /**
     * Validate complete registration request
     */
    public ValidationResult validateRegistrationRequest(EmailRegistrationRequest request) {
        List<String> errors = new ArrayList<>();

        // Validate email
        ValidationResult emailResult = validateEmail(request.getEmail());
        if (!emailResult.isValid()) {
            errors.addAll(emailResult.getErrors());
        }

        // Validate password
        ValidationResult passwordResult = validatePassword(request.getPassword());
        if (!passwordResult.isValid()) {
            errors.addAll(passwordResult.getErrors());
        }

        // Validate first name
        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            errors.add("First name is required");
        }

        // Validate last name
        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            errors.add("Last name is required");
        }

        // Check for duplicate email
        if (emailResult.isValid() && checkDuplicateEmail(request.getEmail())) {
            errors.add("An account with this email address already exists");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Validation result holder
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}
