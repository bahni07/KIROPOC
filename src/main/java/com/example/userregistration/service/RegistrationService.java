package com.example.userregistration.service;

import com.example.userregistration.dto.EmailRegistrationRequest;
import com.example.userregistration.dto.OAuthRegistrationRequest;
import com.example.userregistration.dto.RegistrationResponse;
import com.example.userregistration.dto.VerificationResponse;
import com.example.userregistration.model.CustomerIdentity;
import com.example.userregistration.repository.CustomerIdentityRepository;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class RegistrationService {

    private final CustomerIdentityRepository customerIdentityRepository;
    private final ValidationService validationService;
    private final PasswordHashingService passwordHashingService;
    private final VerificationTokenService verificationTokenService;
    private final EmailService emailService;
    private final EncryptionService encryptionService;

    public RegistrationService(
            CustomerIdentityRepository customerIdentityRepository,
            ValidationService validationService,
            PasswordHashingService passwordHashingService,
            VerificationTokenService verificationTokenService,
            EmailService emailService,
            EncryptionService encryptionService) {
        this.customerIdentityRepository = customerIdentityRepository;
        this.validationService = validationService;
        this.passwordHashingService = passwordHashingService;
        this.verificationTokenService = verificationTokenService;
        this.emailService = emailService;
        this.encryptionService = encryptionService;
    }

    /**
     * Register a new user with email and password
     */
    @Transactional
    public RegistrationResponse registerWithEmail(EmailRegistrationRequest request) {
        // Validate the registration request
        ValidationService.ValidationResult validationResult = 
            validationService.validateRegistrationRequest(request);
        
        if (!validationResult.isValid()) {
            throw new IllegalArgumentException(
                "Validation failed: " + String.join(", ", validationResult.getErrors()));
        }
        
        // Generate verification token
        String verificationToken = verificationTokenService.generateToken();
        String tokenHash = verificationTokenService.hashToken(verificationToken);
        
        // Create customer identity
        CustomerIdentity customer = new CustomerIdentity();
        customer.setEmail(request.getEmail());
        customer.setEmailNormalized(request.getEmail().toLowerCase());
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setPasswordHash(passwordHashingService.hashPassword(request.getPassword()));
        customer.setRegistrationMethod(CustomerIdentity.RegistrationMethod.EMAIL);
        customer.setEmailVerified(false);
        customer.setVerificationTokenHash(tokenHash);
        
        // Save to database
        CustomerIdentity savedCustomer = customerIdentityRepository.save(customer);
        
        // Send verification email
        try {
            emailService.sendVerificationEmail(
                savedCustomer.getEmail(),
                savedCustomer.getFirstName(),
                verificationToken
            );
        } catch (MessagingException e) {
            // Log error but don't fail registration
            // In production, consider using a message queue for retry logic
            throw new RuntimeException("Failed to send verification email: " + e.getMessage(), e);
        }
        
        return new RegistrationResponse(
            savedCustomer.getUserId(),
            savedCustomer.getEmail(),
            false,
            "Registration successful. Please check your email to verify your account.",
            null
        );
    }

    /**
     * Register a new user with OAuth (Google or Amazon)
     */
    @Transactional
    public RegistrationResponse registerWithOAuth(String email, String firstName, String lastName, 
            CustomerIdentity.OAuthProvider provider, String providerId, String accessToken) {
        // Validate email format
        ValidationService.ValidationResult emailValidation = validationService.validateEmail(email);
        if (!emailValidation.isValid()) {
            throw new IllegalArgumentException("Invalid email: " + String.join(", ", emailValidation.getErrors()));
        }
        
        // Check for duplicate email (case-insensitive)
        if (validationService.checkDuplicateEmail(email)) {
            throw new IllegalArgumentException(
                "An account with this email already exists. Please use a different email or sign in.");
        }
        
        // Encrypt OAuth access token if provided
        String encryptedToken = null;
        if (accessToken != null && !accessToken.isEmpty()) {
            encryptedToken = encryptionService.encryptToken(accessToken);
        }
        
        // Create customer identity
        CustomerIdentity customer = new CustomerIdentity();
        customer.setEmail(email);
        customer.setEmailNormalized(email.toLowerCase());
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setRegistrationMethod(CustomerIdentity.RegistrationMethod.OAUTH);
        customer.setOauthProvider(provider);
        customer.setOauthProviderId(providerId);
        customer.setOauthTokenEncrypted(encryptedToken);
        customer.setEmailVerified(true); // OAuth emails are pre-verified
        
        // Save to database
        CustomerIdentity savedCustomer = customerIdentityRepository.save(customer);
        
        // Send welcome email
        try {
            emailService.sendWelcomeEmail(
                savedCustomer.getEmail(),
                savedCustomer.getFirstName()
            );
        } catch (MessagingException e) {
            // Log error but don't fail registration
            // Welcome email is not critical
        }
        
        return new RegistrationResponse(
            savedCustomer.getUserId(),
            savedCustomer.getEmail(),
            true,
            "Registration successful. Welcome!",
            provider.name()
        );
    }

    /**
     * Verify email address using verification token
     */
    @Transactional
    public VerificationResponse verifyEmail(String token) {
        if (token == null || token.isEmpty()) {
            return new VerificationResponse("Invalid verification token", false);
        }
        
        // Hash the token to look up in database
        String tokenHash = verificationTokenService.hashToken(token);
        
        // Find customer by token hash
        Optional<CustomerIdentity> customerOpt = 
            customerIdentityRepository.findByVerificationTokenHash(tokenHash);
        
        if (customerOpt.isEmpty()) {
            return new VerificationResponse("Invalid or expired verification token", false);
        }
        
        CustomerIdentity customer = customerOpt.get();
        
        // Check if already verified (idempotent operation)
        if (customer.isEmailVerified()) {
            return new VerificationResponse("Email already verified", true);
        }
        
        // Check if token is expired (24 hours)
        if (verificationTokenService.isTokenExpired(customer.getCreatedAt())) {
            return new VerificationResponse(
                "Verification token has expired. Please request a new verification email.", false);
        }
        
        // Mark as verified
        customer.setEmailVerified(true);
        customer.setVerificationTokenHash(null); // Clear token after use
        customerIdentityRepository.save(customer);
        
        // Send welcome email
        try {
            emailService.sendWelcomeEmail(customer.getEmail(), customer.getFirstName());
        } catch (MessagingException e) {
            // Log error but don't fail verification
        }
        
        return new VerificationResponse("Email verified successfully. Welcome!", true);
    }

    /**
     * Resend verification email
     */
    @Transactional
    public void resendVerificationEmail(String email) {
        String normalizedEmail = email.toLowerCase();
        
        Optional<CustomerIdentity> customerOpt = 
            customerIdentityRepository.findByEmailNormalized(normalizedEmail);
        
        if (customerOpt.isEmpty()) {
            throw new IllegalArgumentException("No account found with this email");
        }
        
        CustomerIdentity customer = customerOpt.get();
        
        if (customer.isEmailVerified()) {
            throw new IllegalStateException("Email is already verified");
        }
        
        if (customer.getRegistrationMethod() != CustomerIdentity.RegistrationMethod.EMAIL) {
            throw new IllegalStateException("This account was registered with OAuth and doesn't require email verification");
        }
        
        // Generate new verification token
        String verificationToken = verificationTokenService.generateToken();
        String tokenHash = verificationTokenService.hashToken(verificationToken);
        
        customer.setVerificationTokenHash(tokenHash);
        customerIdentityRepository.save(customer);
        
        // Send verification email
        try {
            emailService.sendVerificationEmail(
                customer.getEmail(),
                customer.getFirstName(),
                verificationToken
            );
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send verification email: " + e.getMessage(), e);
        }
    }
}
