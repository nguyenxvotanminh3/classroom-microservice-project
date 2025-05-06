package com.kienlongbank.api;

import java.util.Map;
import java.util.List;

/**
 * User service API to be used via Dubbo
 */
public interface UserService {
    
    /**
     * Get user by ID
     * 
     * @param userId The user ID
     * @return A map containing user details
     */
    Map<String, Object> getUserById(Long userId);
    
    /**
     * Get users by IDs
     * 
     * @param userIds List of user IDs
     * @return List of maps containing user details
     */
    List<Map<String, Object>> getUsersByIds(List<Long> userIds);
    
    /**
     * Check if a user exists
     * 
     * @param userId The user ID
     * @return true if the user exists, false otherwise
     */
    boolean userExists(Long userId);

    /**
     * Get user by username
     * 
     * @param username The username to search for
     * @return A map containing user details
     */
    Map<String, Object> getUserByName(String username);

    String getUserEmailByName(String username);
} 