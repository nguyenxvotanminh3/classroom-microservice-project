package com.kienlongbank.securityservice.integration;

import com.kienlongbank.securityservice.SecurityserviceApplication;
import com.kienlongbank.securityservice.config.TestJwtUtils;
import com.kienlongbank.securityservice.service.SecurityServiceDubboImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = SecurityserviceApplication.class)
@ActiveProfiles("test")
public class SecurityServiceIntegrationTest {

    @Autowired
    private SecurityServiceDubboImpl securityService;

    @Autowired
    private TestJwtUtils jwtUtils;

    @MockBean
    private UserDetailsService userDetailsService;

    private UserDetails testUserDetails;
    private String validToken;
    private String invalidToken = "invalid.jwt.token";
    private String username = "testuser";

    @BeforeEach
    public void setup() {
        // Tạo user test
        testUserDetails = new User(
                username,
                "$2a$10$VpvmZTkz0xk9S5KaBvMRWe4xkBqQj8mJ/JmZJk9Tc5xnK4CEZaXYa", // Encrypted "password123"
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // Tạo valid token từ TestJwtUtils
        validToken = jwtUtils.generateJwtToken(testUserDetails);

        // Mock userDetailsService để trả về testUserDetails
        when(userDetailsService.loadUserByUsername(username)).thenReturn(testUserDetails);
    }

    @Test
    public void testValidateTokenForUsername_ValidTokenAndMatchingUsername() {
        // Test validateTokenForUsername với valid token và username khớp
        boolean result = securityService.validateTokenForUsername(validToken, username);
        
        // Token hợp lệ và username khớp nên kết quả phải là true
        assertTrue(result);
    }

    @Test
    public void testValidateTokenForUsername_ValidTokenButDifferentUsername() {
        // Test validateTokenForUsername với valid token nhưng username khác
        // Tạo token với username khác
        UserDetails otherUser = new User(
                "otheruser",
                "$2a$10$VpvmZTkz0xk9S5KaBvMRWe4xkBqQj8mJ/JmZJk9Tc5xnK4CEZaXYa",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        String tokenWithDifferentUsername = jwtUtils.generateJwtToken(otherUser);
        
        // Test với token và username không khớp
        boolean result = securityService.validateTokenForUsername(tokenWithDifferentUsername, username);
        
        // Token hợp lệ nhưng username không khớp nên kết quả phải là false
        assertFalse(result);
    }

    @Test
    public void testValidateTokenForUsername_InvalidToken() {
        // Test validateTokenForUsername với token không hợp lệ
        boolean result = securityService.validateTokenForUsername(invalidToken, username);
        
        // Token không hợp lệ nên kết quả phải là false
        assertFalse(result);
    }

    @Test
    public void testValidateTokenForUsername_EmptyToken() {
        // Test validateTokenForUsername với token rỗng
        boolean result = securityService.validateTokenForUsername("", username);
        
        // Token rỗng nên kết quả phải là false
        assertFalse(result);
    }

    @Test
    public void testValidateTokenForUsername_NullToken() {
        // Test validateTokenForUsername với token null
        boolean result = securityService.validateTokenForUsername(null, username);
        
        // Token null nên kết quả phải là false
        assertFalse(result);
    }
} 