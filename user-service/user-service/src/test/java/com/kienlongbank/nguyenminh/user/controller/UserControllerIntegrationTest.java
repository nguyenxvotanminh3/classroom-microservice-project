package com.kienlongbank.nguyenminh.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kienlongbank.nguyenminh.config.TestJacksonConfig;
import com.kienlongbank.nguyenminh.config.TestTracerConfig;
import com.kienlongbank.nguyenminh.user.dto.UserRequest;
import com.kienlongbank.nguyenminh.UserServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {UserServiceApplication.class, TestJacksonConfig.class, TestTracerConfig.class})
@AutoConfigureMockMvc // Configure MockMvc
@ActiveProfiles("test") // Activate the 'test' profile (application-test.properties)
@Transactional // Rollback transactions after each test to keep tests isolated
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc; // Inject MockMvc for simulating HTTP requests

    @Autowired
    private ObjectMapper objectMapper; // Inject ObjectMapper for JSON serialization

    @Test
    void contextLoads() {
        // Simple test to ensure the Spring context loads correctly
    }

    @Test
    void shouldCreateUserSuccessfully() throws Exception {
        // Arrange: Create a UserRequest object
        UserRequest userRequest = new UserRequest(
                "integrationtestuser",
                "Integration Test User",
                "integration.test@example.com",
                "password123"
        );
        String userJson = objectMapper.writeValueAsString(userRequest);

        // Act & Assert
        mockMvc.perform(post("/api/users") // Endpoint from UserController @RequestMapping + @PostMapping
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson))
            .andExpect(status().isCreated()) // Expect HTTP 201 Created
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").exists()) // Check for the 'message' field in the response map
            .andExpect(jsonPath("$.data").exists()) // Check for the 'data' field in the response map
            .andExpect(jsonPath("$.data.username").value("integrationtestuser")) // Check username in the nested UserResponse
            .andExpect(jsonPath("$.data.email").value("integration.test@example.com")) // Check email in the nested UserResponse
            .andExpect(jsonPath("$.data.id").exists()); // Check that the created user has an ID

        // Optionally: Add assertions to check if the user was actually saved in the H2 database
        // This would require injecting the UserRepository or UserService and querying it.
        // Example (requires injecting UserRepository):
        // User savedUser = userRepository.findByUsername("integrationtestuser").orElseThrow();
        // assertEquals("Integration Test User", savedUser.getFullName());
    }

} 