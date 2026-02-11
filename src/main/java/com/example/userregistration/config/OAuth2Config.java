package com.example.userregistration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.beans.factory.annotation.Value;

/**
 * OAuth2 Configuration for Google and Amazon providers
 * Configures OAuth2 client registrations with PKCE support
 */
@Configuration
public class OAuth2Config {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;
    
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;
    
    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String googleRedirectUri;
    
    @Value("${spring.security.oauth2.client.registration.amazon.client-id}")
    private String amazonClientId;
    
    @Value("${spring.security.oauth2.client.registration.amazon.client-secret}")
    private String amazonClientSecret;
    
    @Value("${spring.security.oauth2.client.registration.amazon.redirect-uri}")
    private String amazonRedirectUri;

    /**
     * Configure OAuth2 client registrations for Google and Amazon
     */
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(
            googleClientRegistration(),
            amazonClientRegistration()
        );
    }

    /**
     * Google OAuth2 client registration with PKCE
     */
    private ClientRegistration googleClientRegistration() {
        return ClientRegistration.withRegistrationId("google")
            .clientId(googleClientId)
            .clientSecret(googleClientSecret)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri(googleRedirectUri)
            .scope("openid", "profile", "email")
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .tokenUri("https://oauth2.googleapis.com/token")
            .userInfoUri("https://www.googleapis.com/oauth2/v2/userinfo")
            .userNameAttributeName("id")
            .clientName("Google")
            .build();
    }

    /**
     * Amazon OAuth2 client registration with PKCE
     */
    private ClientRegistration amazonClientRegistration() {
        return ClientRegistration.withRegistrationId("amazon")
            .clientId(amazonClientId)
            .clientSecret(amazonClientSecret)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri(amazonRedirectUri)
            .scope("profile")
            .authorizationUri("https://www.amazon.com/ap/oa")
            .tokenUri("https://api.amazon.com/auth/o2/token")
            .userInfoUri("https://api.amazon.com/user/profile")
            .userNameAttributeName("user_id")
            .clientName("Amazon")
            .build();
    }
}
