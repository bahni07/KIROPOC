package com.example.userregistration.repository;

import com.example.userregistration.model.CustomerIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerIdentityRepository extends JpaRepository<CustomerIdentity, UUID> {

    /**
     * Find customer by normalized email (case-insensitive)
     */
    Optional<CustomerIdentity> findByEmailNormalized(String emailNormalized);

    /**
     * Find customer by OAuth provider and provider ID
     */
    Optional<CustomerIdentity> findByOauthProviderAndOauthProviderId(
            CustomerIdentity.OAuthProvider oauthProvider,
            String oauthProviderId
    );

    /**
     * Find customer by verification token hash
     */
    Optional<CustomerIdentity> findByVerificationTokenHash(String verificationTokenHash);

    /**
     * Check if email exists (case-insensitive)
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CustomerIdentity c WHERE LOWER(c.email) = LOWER(:email)")
    boolean existsByEmailIgnoreCase(@Param("email") String email);
}
