package com.kienlongbank.securityservice.api;

import java.util.List;
import java.util.Map;

/**
 * Security service API to be exposed via Dubbo
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
    
    /**
     * Extract roles from a JWT token
     * 
     * @param token The JWT token
     * @return A list of roles
     */
    List<String> extractRoles(String token);
    
    /**
     * Check if a user has a specific role
     * 
     * @param token The JWT token
     * @param role The role to check
     * @return true if the user has the role, false otherwise
     */
    boolean hasRole(String token, String role);
    
    /**
     * Check if a user has any of the specified roles
     * 
     * @param token The JWT token
     * @param roles The roles to check
     * @return true if the user has any of the roles, false otherwise
     */
    boolean hasAnyRole(String token, List<String> roles);
} 