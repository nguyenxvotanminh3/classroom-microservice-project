package com.kienlongbank.securityservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kienlongbank.securityservice.dto.LoginRequest;
import com.kienlongbank.securityservice.handler.AuthHandler;
import com.kienlongbank.securityservice.service.AuthenticateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;

import io.micrometer.tracing.Tracer;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web layer test for AuthController
 */
@WebMvcTest(AuthController.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthHandler authHandler;

    @MockBean
    private Tracer tracer;

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

        when(authHandler.handleAuthenticateUser(any(LoginRequest.class), any()))
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
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Invalid credentials");

        when(authHandler.handleAuthenticateUser(any(LoginRequest.class), any()))
                .thenAnswer(invocation -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    public void testLoginFailure_UserNotFound() throws Exception {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "User not found");

        when(authHandler.handleAuthenticateUser(any(LoginRequest.class), any()))
                .thenAnswer(invocation -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nonExistentUserRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    public void testHealthCheck() throws Exception {
        Map<String, Object> healthResponse = new HashMap<>();
        healthResponse.put("status", "Auth service is running");
        
        when(authHandler.handleHealthCheck())
                .thenAnswer(invocation -> ResponseEntity.ok(healthResponse));
        
        mockMvc.perform(get("/api/auth/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }
} 