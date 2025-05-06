package com.kienlongbank.nguyenminh.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kienlongbank.nguyenminh.user.controller.UserController;
import com.kienlongbank.nguyenminh.user.dto.UserRequest;
import com.kienlongbank.nguyenminh.user.dto.UserResponse;
import com.kienlongbank.nguyenminh.user.exception.CreateUserFallbackException;
import com.kienlongbank.nguyenminh.user.exception.UserException;
import com.kienlongbank.nguyenminh.user.service.UserService;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import com.kienlongbank.nguyenminh.config.TestJacksonConfig;
import com.kienlongbank.nguyenminh.UserServiceApplication;
import org.springframework.test.context.ActiveProfiles;
import com.kienlongbank.nguyenminh.config.TestTracerConfig;

import java.time.LocalDateTime;
import java.util.Locale;

@WebMvcTest(UserController.class)
@Import({TestJacksonConfig.class, TestTracerConfig.class})
@ActiveProfiles("test")
public class UserControllerMockTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private MessageSource messageSource;

    @Mock
    private Span span;

    @Autowired
    private Tracer tracer;

    private UserRequest userRequest;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        // Setup mocked span
        when(tracer.nextSpan()).thenReturn(span);
        when(span.name(any())).thenReturn(span);
        when(span.start()).thenReturn(span);
        when(span.tag(any(), any())).thenReturn(span);

        // Setup test data
        userRequest = new UserRequest();
        userRequest.setUsername("testuser");
        userRequest.setFullName("Test User");
        userRequest.setEmail("test@example.com");
        userRequest.setPassword("password123");

        userResponse = new UserResponse();
        userResponse.setId(1L);
        userResponse.setUsername("testuser");
        userResponse.setFullName("Test User");
        userResponse.setEmail("test@example.com");
        userResponse.setActive(true);
        userResponse.setCreatedAt(LocalDateTime.now());
        userResponse.setUpdatedAt(LocalDateTime.now());

        // Setup message source
        when(messageSource.getMessage("user.create.success", null, Locale.getDefault()))
                .thenReturn("User created successfully");
        when(messageSource.getMessage("user.create.failed.duplicate.username", 
                new Object[]{"testuser", "test@example.com"}, Locale.getDefault()))
                .thenReturn("User creation failed: Username 'testuser' already exists");
        when(messageSource.getMessage("user.create.failed.duplicate.email", 
                new Object[]{"testuser", "test@example.com"}, Locale.getDefault()))
                .thenReturn("User creation failed: Email 'test@example.com' already exists");
        when(messageSource.getMessage("user.create.failed.fallback", null, Locale.getDefault()))
                .thenReturn("User creation could not be completed at this time due to a temporary issue");
    }

    @Test
    void createUser_Success() throws Exception {
        // Setup
        when(userService.createUser(any(UserRequest.class))).thenReturn(userResponse);

        // Execute and Verify
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User created successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    void createUser_UsernameExists() throws Exception {
        // Setup - simulate username already exists
        when(userService.createUser(any(UserRequest.class)))
                .thenThrow(new UserException("Username already exists"));

        // Execute and Verify
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("User creation failed: Username 'testuser' already exists"));
    }

    @Test
    void createUser_EmailExists() throws Exception {
        // Setup - simulate email already exists
        when(userService.createUser(any(UserRequest.class)))
                .thenThrow(new UserException("Email already exists"));

        // Execute and Verify
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("User creation failed: Email 'test@example.com' already exists"));
    }

    @Test
    void createUser_FallbackWithUsernameError() throws Exception {
        // Setup - simulate circuit breaker fallback with username error
        UserException originalException = new UserException("Username already exists");
        CreateUserFallbackException fallbackException = 
                new CreateUserFallbackException("Fallback executed", originalException);
        
        when(userService.createUser(any(UserRequest.class)))
                .thenThrow(fallbackException);

        // Execute and Verify - should show username error, not fallback
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("User creation failed: Username 'testuser' already exists"));
    }

    @Test
    void createUser_FallbackWithEmailError() throws Exception {
        // Setup - simulate circuit breaker fallback with email error
        UserException originalException = new UserException("Email already exists");
        CreateUserFallbackException fallbackException = 
                new CreateUserFallbackException("Fallback executed", originalException);
        
        when(userService.createUser(any(UserRequest.class)))
                .thenThrow(fallbackException);

        // Execute and Verify - should show email error, not fallback
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("User creation failed: Email 'test@example.com' already exists"));
    }

    @Test
    void createUser_GenericFallbackError() throws Exception {
        // Setup - simulate circuit breaker fallback with generic error
        CreateUserFallbackException fallbackException = 
                new CreateUserFallbackException("Fallback executed", new RuntimeException("Database connection error"));
        
        when(userService.createUser(any(UserRequest.class)))
                .thenThrow(fallbackException);

        // Execute and Verify - should show fallback error
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("User creation could not be completed at this time due to a temporary issue"));
    }
} 