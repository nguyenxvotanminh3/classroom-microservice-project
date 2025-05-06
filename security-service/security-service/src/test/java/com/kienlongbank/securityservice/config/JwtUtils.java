package com.kienlongbank.securityservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Mock implementation of JwtUtils for testing purposes
 */
@Component
public class JwtUtils {

    @Value("${security.jwt.secret:testSecretKey12345}")
    private String jwtSecret;

    @Value("${security.jwt.expiration:86400000}")
    private int jwtExpirationMs;

    /**
     * Generate a JWT token for the given user
     * @param userDetails the user details
     * @return a JWT token
     */
    public String generateJwtToken(UserDetails userDetails) {
        // Return a mock token for testing
        return "test-jwt-token-" + userDetails.getUsername();
    }

    /**
     * Get username from JWT token
     * @param token the JWT token
     * @return the username
     */
    public String getUserNameFromJwtToken(String token) {
        // Return a fake username for testing
        if (token.startsWith("test-jwt-token-")) {
            return token.substring("test-jwt-token-".length());
        }
        return "testuser";
    }

    /**
     * Validate a JWT token
     * @param authToken the JWT token
     * @return true if valid, false otherwise
     */
    public boolean validateJwtToken(String authToken) {
        // Always return true for testing
        return true;
    }
} 