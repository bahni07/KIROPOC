package com.example.userregistration.config;

import org.springframework.context.annotation.Configuration;

/**
 * OAuth2 Configuration for Google and Amazon providers
 * OAuth2 clients are configured via application.properties
 * Spring Boot auto-configuration handles client registration
 */
@Configuration
public class OAuth2Config {
    // OAuth2 client registrations are auto-configured from application.properties
    // No manual bean configuration needed
}
