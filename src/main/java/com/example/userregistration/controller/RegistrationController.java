package com.example.userregistration.controller;

import com.example.userregistration.dto.*;
import com.example.userregistration.model.CustomerIdentity;
import com.example.userregistration.service.LoggingService;
import com.example.userregistration.service.OAuth2Service;
import com.example.userregistration.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/register")
public class RegistrationController {

    private final RegistrationService registrationService;
    private final OAuth2Service oauth2Service;
    private final LoggingService loggingService;

    public RegistrationController(
            RegistrationService registrationService,
            OAuth2Service oauth2Service,
            LoggingService loggingService) {
        this.registrationService = registrationService;
        this.oauth2Service = oauth2Service;
        this.loggingService = loggingService;
    }

    /**
     * POST /api/v1/register/email
     * Register a new user with email and password
     */
    @PostMapping("/email")
    public ResponseEntity<RegistrationResponse> registerWithEmail(
            @Valid @RequestBody EmailRegistrationRequest request) {
        try {
            RegistrationResponse response = registrationService.registerWithEmail(request);
            loggingService.logEmailRegistration(request.getEmail(), true);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            loggingService.logValidationError(request.getEmail(), e.getMessage());
            throw e;
        } catch (Exception e) {
            loggingService.logError("Email registration failed for: " + request.getEmail(), e);
            throw new RuntimeException("Registration failed. Please try again later.");
        }
    }

    /**
     * POST /api/v1/register/oauth/initiate
     * Initiate OAuth registration flow
     */
    @PostMapping("/oauth/initiate")
    public ResponseEntity<OAuthAuthorizationResponse> initiateOAuthRegistration(
            @Valid @RequestBody OAuthRegistrationRequest request) {
        try {
            String state = UUID.randomUUID().toString();
            
            OAuthAuthorizationResponse response;
            if (request.getProvider() == OAuthRegistrationRequest.OAuthProviderType.GOOGLE) {
                response = oauth2Service.getGoogleAuthorizationUrl(state);
            } else if (request.getProvider() == OAuthRegistrationRequest.OAuthProviderType.AMAZON) {
                response = oauth2Service.getAmazonAuthorizationUrl(state);
            } else {
                throw new IllegalArgumentException("Unsupported OAuth provider: " + request.getProvider());
            }
            
            loggingService.logInfo("OAuth initiation successful for provider: " + request.getProvider());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            loggingService.logError("OAuth initiation failed", e);
            throw new RuntimeException("OAuth initiation failed. Please try again later.");
        }
    }

    /**
     * GET /api/v1/register/oauth/callback
     * Handle OAuth callback and complete registration
     */
    @GetMapping("/oauth/callback/{provider}")
    public ResponseEntity<RegistrationResponse> handleOAuthCallback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam String state) {
        try {
            String accessToken;
            Map<String, String> userInfo;
            
            if ("google".equalsIgnoreCase(provider)) {
                accessToken = oauth2Service.exchangeGoogleCodeForToken(code, state);
                userInfo = oauth2Service.getGoogleUserInfo(accessToken);
            } else if ("amazon".equalsIgnoreCase(provider)) {
                accessToken = oauth2Service.exchangeAmazonCodeForToken(code, state);
                userInfo = oauth2Service.getAmazonUserInfo(accessToken);
            } else {
                throw new IllegalArgumentException("Unsupported OAuth provider: " + provider);
            }
            
            // Register user with OAuth data
            CustomerIdentity.OAuthProvider oauthProvider = 
                CustomerIdentity.OAuthProvider.valueOf(userInfo.get("provider"));
            
            RegistrationResponse response = registrationService.registerWithOAuth(
                userInfo.get("email"),
                userInfo.get("firstName"),
                userInfo.get("lastName"),
                oauthProvider,
                userInfo.get("providerId"),
                accessToken
            );
            
            loggingService.logOAuthRegistration(userInfo.get("email"), provider, true);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            loggingService.logError("OAuth callback validation failed", e);
            throw e;
        } catch (Exception e) {
            loggingService.logError("OAuth callback failed for provider: " + provider, e);
            throw new RuntimeException("OAuth registration failed. Please try again later.");
        }
    }

    /**
     * GET /api/v1/register/verify
     * Verify email address using token
     */
    @GetMapping("/verify")
    public ResponseEntity<VerificationResponse> verifyEmail(@RequestParam String token) {
        try {
            VerificationResponse response = registrationService.verifyEmail(token);
            loggingService.logEmailVerification("user", response.isSuccess());
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            loggingService.logError("Email verification failed", e);
            throw new RuntimeException("Verification failed. Please try again later.");
        }
    }

    /**
     * POST /api/v1/register/resend-verification
     * Resend verification email
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerificationEmail(
            @RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.isEmpty()) {
                throw new IllegalArgumentException("Email is required");
            }
            
            registrationService.resendVerificationEmail(email);
            loggingService.logInfo("Verification email resent to: " + email);
            
            return ResponseEntity.ok(Map.of(
                "message", "Verification email sent successfully",
                "success", "true"
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            loggingService.logError("Resend verification failed", e);
            throw e;
        } catch (Exception e) {
            loggingService.logError("Resend verification failed", e);
            throw new RuntimeException("Failed to resend verification email. Please try again later.");
        }
    }
}
