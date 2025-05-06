package com.kienlongbank.securityservice.config;

import com.kienlongbank.api.UserService;
import com.kienlongbank.securityservice.service.UserDetailsServiceImpl;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

/**
 * Test configuration to provide mock implementations for testing
 */
@TestConfiguration
public class TestConfig {
    
    /**
     * Provides a mock UserDetailsService for testing
     */
    @Bean
    @Primary
    public UserDetailsService userDetailsService() {
        return new TestUserDetailsService();
    }
    
    /**
     * Provides a mock UserService for testing
     */
    @Bean
    @Primary
    public UserService userService() {
        return new TestUserService();
    }
    
    /**
     * Provides a PasswordEncoder for testing
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * Test implementation of UserDetailsService
     */
    public static class TestUserDetailsService implements UserDetailsService {
        @Override
        public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
            throw new UsernameNotFoundException("Not implemented in test");
        }
    }
    
    /**
     * Test implementation of UserService
     */
    public static class TestUserService implements UserService {
        public Map<String, Object> getUserByName(String username) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("error", "User not found");
            return userMap;
        }
    }
} 