package com.kienlongbank.securityservice.service;

import com.kienlongbank.api.UserService;
import com.kienlongbank.securityservice.config.JwtUtils;
import com.kienlongbank.securityservice.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthenticateServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MessageSource messageSource;

    // Không sử dụng annotation @Mock cho userService vì nó được khởi tạo bằng @DubboReference
    private UserService userService;

    private AuthenticateService authenticateService;

    private LoginRequest loginRequest;
    private UserDetails userDetails;
    private Authentication authentication;

    @BeforeEach
    void setUp() throws Exception {
        // Khởi tạo authenticateService với các mock
        authenticateService = new AuthenticateService(
                authenticationManager, jwtUtils, userDetailsService, passwordEncoder, messageSource);
                
        // Mock UserService và inject vào authenticateService bằng reflection
        userService = mock(UserService.class);
        ReflectionTestUtils.setField(authenticateService, "userService", userService);
        
        // Set up common test data
        loginRequest = new LoginRequest();
        loginRequest.setUserName("testuser");
        loginRequest.setPassword("password123");

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        userDetails = new User("testuser", "encodedPassword", authorities);

        // Không sử dụng authentication trong tất cả các test cases nên không cần stub nó
    }

    @Test
    void authenticateUser_Success() throws Exception {
        // Arrange
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("email", "testuser@example.com");
        
        // Mock authentication cho test case này
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtils.generateJwtToken(any(UserDetails.class))).thenReturn("test.jwt.token");
        when(messageSource.getMessage(eq("login.success"), any(), any(Locale.class)))
                .thenReturn("Login successful");
        when(userService.getUserByName(anyString())).thenReturn(userMap);
        
        // Act
        ResponseEntity<?> response = authenticateService.authenticateUser(loginRequest, Locale.ENGLISH);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertNotNull(responseBody);
        assertEquals("test.jwt.token", responseBody.get("token"));
        assertEquals("testuser", responseBody.get("userName"));
        assertEquals("ROLE_USER", responseBody.get("roles"));
        assertEquals("Login successful", responseBody.get("message"));
        assertEquals(null, responseBody.get("error"));
        assertEquals(true, responseBody.get("success"));
        assertEquals("testuser@example.com", responseBody.get("email"));
        
        // Verify
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userService).getUserByName(anyString());
    }

    @Test
    void authenticateUser_Failure_BadCredentials() throws Exception {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));
        when(messageSource.getMessage(eq("login.failed"), any(), any(Locale.class)))
                .thenReturn("Invalid credentials");
        
        // Act
        ResponseEntity<?> response = authenticateService.authenticateUser(loginRequest, Locale.ENGLISH);
        
        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertNotNull(responseBody);
        assertEquals(null, responseBody.get("token"));
        assertEquals(null, responseBody.get("userName"));
        assertEquals(null, responseBody.get("roles"));
        assertEquals(null, responseBody.get("message"));
        assertEquals("Invalid credentials", responseBody.get("error"));
        assertEquals(false, responseBody.get("success"));
        
        // Verify
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(messageSource).getMessage(eq("login.failed"), any(), any(Locale.class));
        // userService should not be called for failed authentication
        verify(userService, never()).getUserByName(anyString());
    }

    @Test
    void authenticateUser_UserServiceException_FallbackEmail() throws Exception {
        // Arrange
        // Mock authentication cho test case này
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtils.generateJwtToken(any(UserDetails.class))).thenReturn("test.jwt.token");
        when(messageSource.getMessage(eq("login.success"), any(), any(Locale.class)))
                .thenReturn("Login successful");
        when(userService.getUserByName(anyString())).thenThrow(new RuntimeException("Service unavailable"));
        
        // Act
        ResponseEntity<?> response = authenticateService.authenticateUser(loginRequest, Locale.ENGLISH);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertNotNull(responseBody);
        assertEquals("test.jwt.token", responseBody.get("token"));
        assertEquals("testuser", responseBody.get("userName"));
        assertEquals("ROLE_USER", responseBody.get("roles"));
        assertEquals("Login successful", responseBody.get("message"));
        assertEquals(null, responseBody.get("error"));
        assertEquals(true, responseBody.get("success"));
        // Email should fallback to username@example.com
        assertEquals("testuser@example.com", responseBody.get("email"));
        
        // Verify
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userService).getUserByName(anyString());
    }
    
    // Bỏ test cho trường hợp EmergencyUser vì nó phụ thuộc vào getUserEmailByName
    // mà phương thức này không tồn tại trong UserService
} 