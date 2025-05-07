package com.kienlongbank.api;

import java.util.List;
import java.util.Map;

/**
 * Security service API to be used via Dubbo
 */
public interface SecurityService {
    
    /**
     * Authenticate a user and generate a JWT token
     * 
     * @param username The username
     * @param password The password
     * @return A map containing the token and user details
     */
    Map<String, Object> authenticate(String username, String password);
    /**
     * Validate a JWT token
     * 
     * @param token The JWT token to validate
     * @return true if the token is valid, false otherwise
     */
    boolean validateToken(String token);
    /**
     * Get user details from a JWT token
     * 
     * @param token The JWT token
     * @return A map containing user details
     */
    Map<String, Object> getUserDetailsFromToken(String token);
    boolean hasRole(String token, String role);
    boolean hasAnyRole(String token, List<String> roles);
    boolean validateTokenForUsername(String token, String requestedUsername);
    String getUsernameFromToken(String token);
    List<String> extractRoles(String token);

} 