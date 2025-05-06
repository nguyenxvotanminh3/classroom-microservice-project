package com.kienlongbank.securityservice.service;

import com.kienlongbank.api.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @DubboReference(version = "1.0.0", group = "user", check = false, timeout = 5000, retries = 0)
    private UserService userService;
    
    // In-memory cache for emergency use when Dubbo is down
    private final Map<String, UserDetails> userCache = new ConcurrentHashMap<>();
    
    @Value("${emergency.auth.enabled:false}")
    private boolean emergencyAuthEnabled;
    
    @Value("${emergency.auth.username:admin}")
    private String emergencyUsername;
    
    @Value("${emergency.auth.password:$2a$10$4gD78hT7JDT.f2mO/RyiCuo9MnxnKwUJGVRnV5vHsGBnXRcy8jQja}") // Bcrypt for "admin123"
    private String emergencyPassword;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        // Clear any existing cache
        userCache.clear();
        
        // Add emergency user to cache with proper encoding
        UserDetails emergencyUser = User.builder()
                .username(emergencyUsername)
                .password(emergencyPassword)
                .authorities("ADMIN", "USER")
                .build();
        
        userCache.put(emergencyUsername, emergencyUser);
        log.info("Emergency user initialized with username: {}", emergencyUsername);
        log.info("Emergency password (should be BCrypt encoded): {}", emergencyPassword);
    }

    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        log.info("Loading user by username via Dubbo: {}", userName);
        
        // Check if we have this user in cache
        if (userCache.containsKey(userName)) {
            log.info("User found in cache: {}", userName);
            return userCache.get(userName);
        }
        
        // Try to use Dubbo to get the user
        try {
            log.info("Fetching user information for: {}", userName);
            Map<String, Object> userMap = userService.getUserByName(userName);

            if (userMap == null || userMap.containsKey("error")) {
                log.error("User not found with username: {}", userName);
                return tryEmergencyAuth(userName);
            }
            
            log.info("User found via Dubbo: {}", userMap);
            
            // Explicitly cast values from the map
            String username = String.valueOf(userMap.get("username"));
            String password = String.valueOf(userMap.get("password"));
            log.info("User found via Dubbo: {}", username);
            log.info("User found via password: {}", password);
            if (username == null || "null".equals(username) || password == null || "null".equals(password)) {
                log.error("Invalid user data (missing username or password) for: {}", userName);
                return tryEmergencyAuth(userName);
            }
            
            UserDetails userDetails = User.builder()
                    .username(username)
                    .password(password)
                    .authorities("USER")
                    .build();
                    
            // Cache the user details for future use if Dubbo is down
            userCache.put(username, userDetails);
            
            return userDetails;
        } catch (Exception e) {
            log.error("Error loading user by username: {}", userName, e);
            return tryEmergencyAuth(userName);
        }
    }

    private UserDetails tryEmergencyAuth(String userName) {
        // Emergency authentication as a fallback when Dubbo is down
        log.warn("Attempting emergency authentication for user: {}, emergencyEnabled: {}, emergencyUsername: {}", 
                userName, emergencyAuthEnabled, emergencyUsername);
                
        // Always allow the configured emergency user regardless of enabled setting
        // This ensures authentication works even when Dubbo is down
        if (userName.equals(emergencyUsername)) {
            log.warn("Using emergency authentication for user: {}", userName);
            return User.builder()
                    .username(emergencyUsername)
                    .password(emergencyPassword)
                    .authorities("ADMIN", "USER")
                    .build();
        }
        
        log.warn("Emergency auth used but username doesn't match emergency username: {} vs {}", 
                 userName, emergencyUsername);
        
        // For any other user, throw exception
        throw new UsernameNotFoundException("User not found with username: " + userName);
    }
}