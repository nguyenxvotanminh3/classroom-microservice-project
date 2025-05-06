package com.kienlongbank.apigateway.client;

import com.kienlongbank.api.SecurityService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class SecurityServiceClient {


    @DubboReference(version = "1.0.0", group = "security", check = false, timeout = 5000, retries = 0)
    private SecurityService securityService;
    
    /**
     * Validate a JWT token
     *
     * @param token The JWT token to validate
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            log.debug("Validating token using SecurityService");
            return securityService.validateToken(token);
        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get user details from token
     *
     * @param token The JWT token
     * @return A map containing user details
     */
    public Map<String, Object> getUserDetailsFromToken(String token) {
        try {
            log.debug("Getting user details from token using SecurityService");
            return securityService.getUserDetailsFromToken(token);
        } catch (Exception e) {
            log.error("Error getting user details from token: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Extract roles from a token
     *
     * @param token The JWT token
     * @return A list of roles
     */

    public List<String> extractRoles(String token) {
        try {
            log.debug("Extracting roles from token");
            return Collections.singletonList(securityService.extractRoles(token).toString());
        } catch (Exception e) {
            log.error("Error extracting roles from token: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Check if token has a specific role
     *
     * @param token The JWT token
     * @param role The role to check
     * @return true if the token has the role, false otherwise
     */
    public boolean hasRole(String token, String role) {
        try {
            log.debug("Checking if token has role: {} using JwtValidationService", role);
            return securityService.hasRole(token, role);
        } catch (Exception e) {
            log.error("Error checking role: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if token has any of the specified roles
     *
     * @param token The JWT token
     * @param roles The roles to check
     * @return true if the token has any of the roles, false otherwise
     */
    public boolean hasAnyRole(String token, List<String> roles) {
        try {
            log.debug("Checking if token has any of roles: {} using JwtValidationService", roles);
            return securityService.hasAnyRole(token, roles);
        } catch (Exception e) {
            log.error("Error checking roles: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get username from token
     *
     * @param token The JWT token
     * @return The username or null if token is invalid
     */
    public String getUsernameFromToken(String token) {
        try {
            Map<String, Object> userDetails = securityService.getUserDetailsFromToken(token);
            return (String) userDetails.get("username");
        } catch (Exception e) {
            // Fallback to JwtValidationService if SecurityService fails
            try {
                log.debug("Falling back to JwtValidationService for getting username");
                return securityService.getUsernameFromToken(token);
            } catch (Exception ex) {
                log.error("Error getting username from token: {}", ex.getMessage());
                return null;
            }
        }
    }
} 