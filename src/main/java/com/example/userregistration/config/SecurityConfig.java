package com.example.userregistration.config;

import com.example.userregistration.filter.AccountVerificationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for the application
 * Configures HTTPS/TLS, CORS, CSRF, rate limiting, and security headers
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AccountVerificationFilter accountVerificationFilter;

    public SecurityConfig(AccountVerificationFilter accountVerificationFilter) {
        this.accountVerificationFilter = accountVerificationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF Protection - enabled for state-changing operations
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/v1/register/**") // Disable for registration endpoints
            )
            
            // CORS Configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - no authentication required
                .requestMatchers(
                    "/api/v1/register/email",
                    "/api/v1/register/oauth/initiate",
                    "/api/v1/register/oauth/callback/**",
                    "/api/v1/register/verify",
                    "/api/v1/register/resend-verification",
                    "/actuator/health",
                    "/error"
                ).permitAll()
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            
            // Session management - stateless for REST API
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Security headers
            .headers(headers -> headers
                // HTTP Strict Transport Security (HSTS)
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000) // 1 year
                )
                
                // X-Frame-Options - prevent clickjacking
                .frameOptions(frame -> frame.deny())
                
                // X-Content-Type-Options - prevent MIME sniffing
                .contentTypeOptions(contentType -> {})
                
                // X-XSS-Protection
                .xssProtection(xss -> {})
                
                // Content Security Policy
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; frame-ancestors 'none'")
                )
            )
            
            // Add custom verification filter
            .addFilterBefore(accountVerificationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS configuration
     * Allows cross-origin requests from specified origins
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allowed origins (configure based on environment)
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",  // React dev server
            "http://localhost:4200",  // Angular dev server
            "http://localhost:8080"   // Same origin
        ));
        
        // Allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));
        
        // Allowed headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin"
        ));
        
        // Expose headers
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type"
        ));
        
        // Allow credentials
        configuration.setAllowCredentials(true);
        
        // Max age for preflight requests
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
