package com.example.userregistration.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customer_identity")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @NotBlank(message = "Normalized email is required")
    @Size(max = 255, message = "Normalized email must not exceed 255 characters")
    @Column(name = "email_normalized", nullable = false, unique = true, length = 255)
    private String emailNormalized;

    @Size(max = 255, message = "Password hash must not exceed 255 characters")
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @NotNull(message = "Registration method is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "registration_method", nullable = false, length = 20)
    private RegistrationMethod registrationMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider", length = 20)
    private OAuthProvider oauthProvider;

    @Size(max = 255, message = "OAuth provider ID must not exceed 255 characters")
    @Column(name = "oauth_provider_id", length = 255)
    private String oauthProviderId;

    @Column(name = "oauth_token_encrypted", columnDefinition = "TEXT")
    private String oauthTokenEncrypted;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Size(max = 255, message = "Verification token hash must not exceed 255 characters")
    @Column(name = "verification_token_hash", length = 255)
    private String verificationTokenHash;

    @Column(name = "verification_token_expires_at")
    private LocalDateTime verificationTokenExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (emailNormalized == null && email != null) {
            emailNormalized = email.toLowerCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum RegistrationMethod {
        EMAIL,
        OAUTH
    }

    public enum OAuthProvider {
        GOOGLE,
        AMAZON
    }
}
