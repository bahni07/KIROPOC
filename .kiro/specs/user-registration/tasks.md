# Implementation Plan: User Registration

## Overview

This implementation plan breaks down the user registration feature into incremental, testable steps. The implementation will use Java with Spring Boot for the REST API, Spring Security for authentication, PostgreSQL for data persistence, and JavaMail for email services. Property-based testing will use jqwik, and unit testing will use JUnit 5.

## Tasks

- [x] 1. Set up project structure and dependencies
  - Create Spring Boot project with Maven/Gradle
  - Add dependencies: Spring Web, Spring Data JPA, Spring Security, PostgreSQL driver, jqwik, JUnit 5, JavaMail, bcrypt
  - Configure application.properties for database and email
  - Set up package structure: controller, service, repository, model, dto, config, exception
  - _Requirements: All_

- [x] 2. Implement domain models and database schema
  - [x] 2.1 Create CustomerIdentity entity class
    - Define all fields matching the database schema
    - Add JPA annotations (@Entity, @Table, @Column, @Id)
    - Add validation constraints
    - Implement constructors, getters, setters
    - _Requirements: 1.1, 2.4, 3.1, 5.1, 5.4_
  
  - [x] 2.2 Create database migration script
    - Write SQL DDL for customer_identity table
    - Add indexes for email_normalized, oauth_provider_id, verification_token_hash
    - Add constraints for registration_method, oauth_provider, password_hash
    - _Requirements: 1.1, 2.4, 4.1_
  
  - [x] 2.3 Create DTO classes for API requests/responses
    - EmailRegistrationRequest DTO
    - OAuthRegistrationRequest DTO
    - RegistrationResponse DTO
    - ErrorResponse DTO
    - _Requirements: 1.1, 2.4, 6.1_

- [x] 3. Implement repository layer
  - [x] 3.1 Create CustomerIdentityRepository interface
    - Extend JpaRepository
    - Add custom query methods: findByEmailNormalized, findByOauthProviderAndOauthProviderId, findByVerificationTokenHash
    - _Requirements: 1.1, 1.5, 2.4, 3.3, 4.1_
  
  - [ ]* 3.2 Write unit tests for repository queries
    - Test findByEmailNormalized with various cases
    - Test OAuth provider queries
    - Test verification token queries
    - _Requirements: 1.5, 4.1_

- [x] 4. Implement validation service
  - [x] 4.1 Create ValidationService class
    - Implement validateEmail method (RFC 5322 compliance)
    - Implement validatePassword method (complexity requirements)
    - Implement checkDuplicateEmail method (case-insensitive)
    - Implement validateRegistrationRequest method
    - _Requirements: 1.3, 1.4, 1.5, 4.1, 6.1, 6.2_
  
  - [ ]* 4.2 Write property test for password validation
    - **Property 3: Password validation enforces complexity requirements**
    - **Validates: Requirements 1.3, 1.4**
    - Generate random valid and invalid passwords
    - Verify validation correctly identifies compliance
    - Verify descriptive error messages for failures
  
  - [ ]* 4.3 Write property test for email format validation
    - **Property 16: Email format validation**
    - **Validates: Requirements 6.1, 6.2**
    - Generate random valid and invalid email strings
    - Verify RFC 5322 compliance checking
    - Verify appropriate error messages
  
  - [ ]* 4.4 Write property test for case-insensitive duplicate detection
    - **Property 4: Case-insensitive duplicate email prevention**
    - **Validates: Requirements 1.5, 4.1**
    - Generate random email with case variations
    - Create account with one case, attempt registration with different case
    - Verify rejection with appropriate error
  
  - [ ]* 4.5 Write unit tests for validation edge cases
    - Test empty fields
    - Test SQL injection attempts in email
    - Test boundary values (exactly 8 char password)
    - Test special characters in names
    - _Requirements: 1.3, 1.4, 6.1, 6.2, 6.4_

- [x] 5. Implement password hashing service
  - [x] 5.1 Create PasswordHashingService class
    - Implement hashPassword method using BCrypt with cost factor 12
    - Implement verifyPassword method
    - _Requirements: 5.1, 5.2_
  
  - [ ]* 5.2 Write property test for password hashing
    - **Property 13: Password hashing**
    - **Validates: Requirements 5.1, 5.2**
    - Generate random passwords
    - Verify stored values are valid BCrypt hashes
    - Verify hashes are different for same password (different salts)
    - Verify plain text passwords are never stored
  
  - [ ]* 5.3 Write unit tests for password hashing
    - Test hash generation
    - Test password verification
    - Test cost factor is 12
    - _Requirements: 5.1, 5.2_

- [x] 6. Implement verification token service
  - [x] 6.1 Create VerificationTokenService class
    - Implement generateToken method (32-byte secure random)
    - Implement hashToken method (SHA-256)
    - Implement isTokenExpired method (24-hour expiration)
    - _Requirements: 3.1, 3.4_
  
  - [ ]* 6.2 Write property test for token generation
    - **Property 9: Verification token generation and email delivery**
    - **Validates: Requirements 3.1, 3.2**
    - Generate random registrations
    - Verify unique tokens are generated
    - Verify token hashes are stored
    - Verify tokens appear in verification emails
  
  - [ ]* 6.3 Write unit tests for token service
    - Test token uniqueness
    - Test token expiration logic
    - Test hash generation
    - _Requirements: 3.1, 3.4_

- [x] 7. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Implement email service
  - [x] 8.1 Create EmailService class
    - Implement sendVerificationEmail method
    - Implement sendWelcomeEmail method
    - Create email templates (HTML and plain text)
    - Configure JavaMail with SMTP settings
    - _Requirements: 1.2, 3.2_
  
  - [ ]* 8.2 Write property test for verification email delivery
    - **Property 2: Valid email registration triggers verification email**
    - **Validates: Requirements 1.2**
    - Generate random registration data
    - Verify email service is invoked with correct email
    - Verify email contains verification link
  
  - [ ]* 8.3 Write unit tests for email service
    - Test email content generation
    - Test template rendering
    - Test email delivery failure handling
    - Mock SMTP server for testing
    - _Requirements: 1.2, 3.2_

- [x] 9. Implement OAuth2 integration service
  - [x] 9.1 Create OAuth2Service class
    - Implement getAuthorizationUrl method for Google
    - Implement getAuthorizationUrl method for Amazon
    - Implement exchangeCodeForToken method
    - Implement getUserInfo method
    - Configure OAuth2 client IDs and secrets
    - _Requirements: 2.1, 2.2, 2.3_
  
  - [x] 9.2 Create OAuth2 configuration class
    - Configure Google OAuth2 client
    - Configure Amazon OAuth2 client
    - Implement PKCE for enhanced security
    - _Requirements: 2.1, 2.2_
  
  - [ ]* 9.3 Write property test for OAuth data extraction
    - **Property 8: OAuth callback extracts user data**
    - **Validates: Requirements 2.3**
    - Generate random OAuth provider responses
    - Verify correct extraction of email, firstName, lastName, providerId
    - Verify data is properly stored
  
  - [ ]* 9.4 Write unit tests for OAuth integration
    - Test Google OAuth flow with mock responses
    - Test Amazon OAuth flow with mock responses
    - Test OAuth error handling (denied access, network failure)
    - Test token exchange failures
    - _Requirements: 2.1, 2.2, 2.3_

- [x] 10. Implement encryption service for OAuth tokens
  - [x] 10.1 Create EncryptionService class
    - Implement encryptToken method using AES-256-GCM
    - Implement decryptToken method
    - Configure encryption key from secure key management
    - _Requirements: 5.4_
  
  - [ ]* 10.2 Write property test for OAuth token encryption
    - **Property 14: OAuth token encryption**
    - **Validates: Requirements 5.4**
    - Generate random OAuth tokens
    - Verify stored values are encrypted (not plain text)
    - Verify decryption returns original token
  
  - [ ]* 10.3 Write unit tests for encryption service
    - Test encryption/decryption round trip
    - Test encryption produces different ciphertext for same input
    - Test decryption with wrong key fails
    - _Requirements: 5.4_

- [x] 11. Implement registration service
  - [x] 11.1 Create RegistrationService class
    - Implement registerWithEmail method
    - Implement registerWithOAuth method
    - Implement verifyEmail method
    - Orchestrate validation, hashing, token generation, email sending
    - _Requirements: 1.1, 1.2, 2.4, 3.3_
  
  - [ ]* 11.2 Write property test for email registration creates record
    - **Property 1: Valid email registration creates database record**
    - **Validates: Requirements 1.1**
    - Generate random unique emails and valid passwords
    - Verify database record is created with correct data
    - Verify password is hashed
    - Verify email is normalized
  
  - [ ]* 11.3 Write property test for OAuth registration creates record
    - **Property 5: OAuth registration with unique email creates account**
    - **Validates: Requirements 2.4**
    - Generate random OAuth user data with unique emails
    - Verify database record is created
    - Verify email is marked as verified
    - Verify OAuth provider info is stored
  
  - [ ]* 11.4 Write property test for cross-method duplicate prevention
    - **Property 6: Cross-method duplicate prevention**
    - **Validates: Requirements 2.5, 4.2**
    - Create account via one method
    - Attempt registration via different method with same email
    - Verify rejection
  
  - [ ]* 11.5 Write property test for duplicate error consistency
    - **Property 7: Duplicate rejection error consistency**
    - **Validates: Requirements 4.3**
    - Generate duplicate attempts across methods
    - Verify error messages don't reveal registration method
  
  - [ ]* 11.6 Write property test for valid token verification
    - **Property 10: Valid token verification**
    - **Validates: Requirements 3.3**
    - Generate random valid tokens
    - Verify account is marked as verified
    - Verify idempotency (multiple verifications succeed)
  
  - [ ]* 11.7 Write property test for invalid token rejection
    - **Property 11: Invalid token rejection**
    - **Validates: Requirements 3.4**
    - Generate invalid and expired tokens
    - Verify rejection with error message
    - Verify account status unchanged
  
  - [ ]* 11.8 Write unit tests for registration service
    - Test successful email registration flow
    - Test successful OAuth registration flow
    - Test duplicate email rejection
    - Test verification token expiration
    - Test email service failure handling
    - _Requirements: 1.1, 1.2, 2.4, 3.3, 3.4_

- [x] 12. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 13. Implement access control for unverified accounts
  - [x] 13.1 Create AccountVerificationFilter
    - Implement filter to check email verification status
    - Block access to protected endpoints for unverified accounts
    - Allow access to verification and public endpoints
    - _Requirements: 3.5_
  
  - [ ]* 13.2 Write property test for unverified account access restriction
    - **Property 12: Unverified account access restriction**
    - **Validates: Requirements 3.5**
    - Generate random unverified accounts
    - Attempt to access protected resources
    - Verify access is denied
  
  - [ ]* 13.3 Write unit tests for access control
    - Test unverified account blocked from protected endpoints
    - Test verified account allowed access
    - Test verification endpoint always accessible
    - _Requirements: 3.5_

- [x] 14. Implement logging with sensitive data exclusion
  - [x] 14.1 Create LoggingService class
    - Implement sanitizeLogMessage method
    - Configure log patterns to exclude passwords and tokens
    - Add logging to all registration operations
    - _Requirements: 5.5_
  
  - [ ]* 14.2 Write property test for log sanitization
    - **Property 15: Sensitive data exclusion from logs**
    - **Validates: Requirements 5.5**
    - Generate random registration events
    - Capture log output
    - Verify logs don't contain passwords or tokens
  
  - [ ]* 14.3 Write unit tests for logging
    - Test password is not logged
    - Test OAuth tokens are not logged
    - Test registration events are logged with sanitized data
    - _Requirements: 5.5_

- [x] 15. Implement REST API controllers
  - [x] 15.1 Create RegistrationController class
    - Implement POST /api/v1/register/email endpoint
    - Implement POST /api/v1/register/oauth/initiate endpoint
    - Implement POST /api/v1/register/oauth/callback endpoint
    - Implement GET /api/v1/register/verify endpoint
    - Add request validation annotations
    - Add error handling with @ExceptionHandler
    - _Requirements: 1.1, 2.1, 2.2, 3.3, 6.1, 6.4_
  
  - [x] 15.2 Create GlobalExceptionHandler class
    - Handle validation exceptions (400)
    - Handle duplicate email exceptions (400)
    - Handle authentication exceptions (401)
    - Handle server exceptions (500)
    - Return consistent ErrorResponse format
    - _Requirements: 1.4, 1.5, 3.4_
  
  - [ ]* 15.3 Write unit tests for REST controllers
    - Test successful email registration endpoint
    - Test successful OAuth initiation endpoint
    - Test successful OAuth callback endpoint
    - Test successful verification endpoint
    - Test validation error responses
    - Test duplicate email error responses
    - Mock service layer
    - _Requirements: 1.1, 1.4, 1.5, 2.1, 2.2, 3.3, 3.4_

- [x] 16. Implement security configuration
  - [x] 16.1 Create SecurityConfig class
    - Configure HTTPS/TLS enforcement
    - Configure CORS settings
    - Configure CSRF protection
    - Configure rate limiting for registration endpoints
    - Add security headers (HSTS, X-Frame-Options, etc.)
    - _Requirements: 5.3_
  
  - [ ]* 16.2 Write unit tests for security configuration
    - Test HTTPS enforcement
    - Test CSRF protection
    - Test rate limiting
    - Test security headers
    - _Requirements: 5.3_

- [ ] 17. Integration and end-to-end testing
  - [ ]* 17.1 Write integration test for complete email registration flow
    - Test registration → email sent → verification → account verified
    - Use test database and mock email service
    - _Requirements: 1.1, 1.2, 3.1, 3.2, 3.3_
  
  - [ ]* 17.2 Write integration test for complete OAuth registration flow
    - Test OAuth initiation → callback → account creation
    - Use mock OAuth providers
    - _Requirements: 2.1, 2.2, 2.3, 2.4_
  
  - [ ]* 17.3 Write integration test for duplicate prevention
    - Test email registration followed by OAuth with same email
    - Test OAuth registration followed by email with same email
    - Verify both directions are blocked
    - _Requirements: 1.5, 2.5, 4.1, 4.2_
  
  - [ ]* 17.4 Write integration test for error handling
    - Test database failure recovery
    - Test email service failure handling
    - Test OAuth provider failure handling
    - _Requirements: All error handling_

- [ ] 18. Final checkpoint - Ensure all tests pass
  - Run all unit tests
  - Run all property-based tests
  - Run all integration tests
  - Verify code coverage meets 85% minimum
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Property-based tests use jqwik with minimum 100 iterations
- Unit tests use JUnit 5 with Mockito for mocking
- Integration tests use Spring Boot Test with TestContainers for PostgreSQL
- All tests must pass before proceeding to next checkpoint
- Security configuration should be reviewed by security team before production deployment
