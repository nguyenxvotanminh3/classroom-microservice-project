package com.kienlongbank.classroomservice.client;

import com.kienlongbank.api.SecurityService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class SecurityServiceClient {

    @DubboReference(version = "1.0.0", group = "security", check = false, timeout = 5000)
    private SecurityService securityService;

    /**
     * Authenticate a user via the security service
     *
     * @param username The username
     * @param password The password
     * @return A map containing authentication results
     */
    public Map<String, Object> authenticate(String username, String password) {
        try {
            log.info("Calling security service to authenticate user: {}", username);
            return securityService.authenticate(username, password);
        } catch (Exception e) {
            log.error("Error calling security service for authentication", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "error");
            errorResult.put("message", "Error connecting to security service: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * Validate a JWT token via the security service
     *
     * @param token The JWT token to validate
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            log.info("Calling security service to validate token");
            return securityService.validateToken(token);
        } catch (Exception e) {
            log.error("Error calling security service for token validation", e);
            return false;
        }
    }

    /**
     * Get user details from a JWT token via the security service
     *
     * @param token The JWT token
     * @return A map containing user details
     */
    public Map<String, Object> getUserDetailsFromToken(String token) {
        try {
            log.info("Calling security service to get user details from token");
            return securityService.getUserDetailsFromToken(token);
        } catch (Exception e) {
            log.error("Error calling security service for getting user details", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Error connecting to security service: " + e.getMessage());
            return errorResult;
        }
    }
}