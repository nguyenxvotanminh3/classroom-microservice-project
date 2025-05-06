package com.kienlongbank.securityservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kienlongbank.securityservice.dto.LoginRequest;
import com.kienlongbank.securityservice.service.AuthenticateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for AuthController
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
        "spring.main.allow-bean-definition-overriding=true"
    }
)
@ActiveProfiles("test")
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean(name = "authenticateService")
    private AuthenticateService authenticateService;

    @Autowired
    private ObjectMapper objectMapper;

    private LoginRequest validRequest;
    private LoginRequest invalidRequest;
    private LoginRequest nonExistentUserRequest;

    @BeforeEach
    public void setup() {
        // Valid request
        validRequest = new LoginRequest();
        validRequest.setUserName("testuser");
        validRequest.setPassword("password123");

        // Invalid password request
        invalidRequest = new LoginRequest();
        invalidRequest.setUserName("testuser");
        invalidRequest.setPassword("wrongpassword");

        // Non-existent user request
        nonExistentUserRequest = new LoginRequest();
        nonExistentUserRequest.setUserName("nonexistentuser");
        nonExistentUserRequest.setPassword("password123");
    }

    @Test
    public void testLoginSuccess() throws Exception {
        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("token", "jwt-token");
        tokenResponse.put("username", "testuser");
        tokenResponse.put("roles", "ROLE_USER");

        when(authenticateService.authenticateUser(any(LoginRequest.class), any(Locale.class)))
            .thenAnswer(invocation -> ResponseEntity.ok(tokenResponse));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.roles").value("ROLE_USER"));
    }

    @Test
    public void testLoginFailure_InvalidCredentials() throws Exception {
        // Mock the authenticate service to throw exception for invalid credentials
        when(authenticateService.authenticateUser(any(LoginRequest.class), any(Locale.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Test the controller using MockMvc
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    public void testLoginFailure_UserNotFound() throws Exception {
        // Mock the authenticate service to throw exception for non-existent user
        when(authenticateService.authenticateUser(any(LoginRequest.class), any(Locale.class)))
                .thenThrow(new UsernameNotFoundException("User not found"));

        // Test the controller using MockMvc
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nonExistentUserRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    public void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/auth/health"))
                .andExpect(status().isOk());
    }

//    @Test
//    public void testLogout() throws Exception {
//        // Gửi request logout, kiểm tra status OK (có thể kiểm tra thêm logic nếu cần)
//        mockMvc.perform(post("/api/auth/logout"))
//                .andExpect(status().isOk());
//    }
}