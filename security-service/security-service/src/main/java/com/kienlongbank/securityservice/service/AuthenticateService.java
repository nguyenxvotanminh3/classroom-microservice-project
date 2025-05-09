package com.kienlongbank.securityservice.service;


import com.kienlongbank.api.UserService;
import com.kienlongbank.securityservice.config.JwtUtils;
import com.kienlongbank.securityservice.dto.JwtResponse;
import com.kienlongbank.securityservice.dto.LoginRequest;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.context.MessageSource;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

@Service
@Slf4j
public class AuthenticateService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final MessageSource messageSource;
    
    @DubboReference(version = "1.0.0", group = "user", check = false, timeout = 5000, retries = 0)
    private UserService userService;

    public AuthenticateService(AuthenticationManager authenticationManager, JwtUtils jwtUtils, UserDetailsService userDetailsService, PasswordEncoder passwordEncoder, MessageSource messageSource) {

        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.messageSource = messageSource;
    }


    public ResponseEntity<?> authenticateUser(LoginRequest loginRequest, Locale locale) {
        log.info("Authentication request for user: {}", loginRequest.getUserName());
        try {
            // Manual check for emergency user
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getUserName());
                
                // If emergency user, manually verify password
                if (userDetails != null && passwordEncoder.matches(loginRequest.getPassword(), userDetails.getPassword())) {
                    log.info("Emergency user authenticated: {}", loginRequest.getUserName());

                    // Generate JWT for emergency user
                    String jwt = jwtUtils.generateJwtToken(userDetails);
                    String username = userDetails.getUsername();
                    String roles = userDetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.joining(","));
                    
                    String msg;
                    try {
                        msg = messageSource.getMessage("login.success", null, "Login successful", locale);
                    } catch (Exception e) {
                        log.warn("Could not load message 'login.success': {}", e.getMessage());
                        msg = "Login successful";
                    }
                    
                    // Get user email
                    String email = "";
                    try {
                        email = userService.getUserEmailByName(loginRequest.getUserName());
                        log.info("Retrieved email: {}", email);
                    } catch (Exception e) {
                        log.error("Error getting user email: {}", e.getMessage());
                        email = username + "@example.com"; // Fallback
                    }

                    Map<String, Object> response = new HashMap<>();
                    response.put("token", jwt);
                    response.put("userName", username);
                    response.put("roles", roles);
                    response.put("message", msg);
                    response.put("error", null);
                    response.put("success", true);
                    response.put("email", email);
                    return ResponseEntity.ok(response);
                } else {
                    log.warn("Password verification failed for emergency user: {}", loginRequest.getUserName());
                    // Fall through to standard authentication
                }
            } catch (Exception e) {
                log.error("Error during emergency user authentication: {}", e.getMessage());
                // Fall through to standard authentication
            }
            
            // Standard authentication if not emergency user or emergency authentication failed
            Authentication authentication;
            try {
                authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(loginRequest.getUserName(), loginRequest.getPassword())
                );
            } catch (AuthenticationException e) {
                log.error("Standard authentication failed: {}", e.getMessage());
                throw e; // Re-throw for consistent error handling
            }

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.info("User {} authenticated successfully", loginRequest.getUserName());
            
            // Lấy thông tin UserDetails từ authentication
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // Tạo JWT
            String jwt = jwtUtils.generateJwtToken(userDetails);
            String username = userDetails.getUsername();
            String roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(","));
            
            String msg;
            try {
                msg = messageSource.getMessage("login.success", null, "Login successful", locale);
            } catch (Exception e) {
                log.warn("Could not load message 'login.success': {}", e.getMessage());
                msg = "Login successful";
            }
            
            // Get user email from UserService
            String email = "";
            try {
                Map<String, Object> userMap = userService.getUserByName(username);
                if (userMap != null && userMap.containsKey("email")) {
                    email = String.valueOf(userMap.get("email"));
                }
                log.info("Retrieved email from user service: {}", email);
            } catch (Exception e) {
                log.error("Error getting user email: {}", e.getMessage());
                email = username + "@example.com"; // Fallback
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("userName", username);
            response.put("roles", roles);
            response.put("message", msg);
            response.put("error", null);
            response.put("success", true);
            response.put("email", email);
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            log.error("Authentication failed: {}", e.getMessage(), e);
            String msg;
            try {
                msg = messageSource.getMessage("login.failed", null, "Invalid username or password", locale);
            } catch (Exception ex) {
                log.warn("Could not load message 'login.failed': {}", ex.getMessage());
                msg = "Invalid username or password";
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", null);
            response.put("userName", null);
            response.put("roles", null);
            response.put("message", null);
            response.put("error", msg);
            response.put("success", false);
            response.put("email", null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            log.error("Unexpected error during authentication: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "An unexpected error occurred. Please try again later.");
            response.put("success", false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}