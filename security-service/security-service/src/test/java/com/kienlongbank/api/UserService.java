package com.kienlongbank.api;

import java.util.Map;

/**
 * Test mock for the UserService interface
 * This is a simplified version for testing purposes
 */
public interface UserService {
    /**
     * Get user by username
     * @param username the username to search for
     * @return A map containing user details
     */
    Map<String, Object> getUserByName(String username);
} 