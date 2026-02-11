package com.example.userregistration.filter;

import com.example.userregistration.model.CustomerIdentity;
import com.example.userregistration.repository.CustomerIdentityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Filter to check email verification status and block unverified accounts
 * from accessing protected endpoints
 */
@Component
public class AccountVerificationFilter extends OncePerRequestFilter {

    private final CustomerIdentityRepository customerIdentityRepository;
    private final ObjectMapper objectMapper;
    
    // Endpoints that don't require email verification
    private static final List<String> ALLOWED_PATHS = Arrays.asList(
        "/api/v1/register/email",
        "/api/v1/register/oauth/initiate",
        "/api/v1/register/oauth/callback",
        "/api/v1/register/verify",
        "/api/v1/register/resend-verification",
        "/actuator",
        "/error",
        "/public"
    );

    public AccountVerificationFilter(CustomerIdentityRepository customerIdentityRepository) {
        this.customerIdentityRepository = customerIdentityRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
            FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        
        // Allow access to public endpoints
        if (isAllowedPath(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Get authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // If not authenticated, let Spring Security handle it
        if (authentication == null || !authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getPrincipal())) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Get user email from authentication
        String email = authentication.getName();
        
        // Look up user in database
        Optional<CustomerIdentity> customerOpt = 
            customerIdentityRepository.findByEmailNormalized(email.toLowerCase());
        
        if (customerOpt.isEmpty()) {
            // User not found, let Spring Security handle it
            filterChain.doFilter(request, response);
            return;
        }
        
        CustomerIdentity customer = customerOpt.get();
        
        // Check if email is verified
        if (!customer.isEmailVerified()) {
            // Block access for unverified accounts
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Email not verified");
            errorResponse.put("message", "Please verify your email address before accessing this resource");
            errorResponse.put("verified", false);
            
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return;
        }
        
        // Email is verified, allow access
        filterChain.doFilter(request, response);
    }
    
    /**
     * Check if the request path is in the allowed list
     */
    private boolean isAllowedPath(String requestPath) {
        return ALLOWED_PATHS.stream().anyMatch(requestPath::startsWith);
    }
}
