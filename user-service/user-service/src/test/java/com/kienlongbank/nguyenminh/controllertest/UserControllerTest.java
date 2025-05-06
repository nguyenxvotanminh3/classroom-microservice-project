package com.kienlongbank.nguyenminh.controllertest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kienlongbank.nguyenminh.user.controller.UserController;
import com.kienlongbank.nguyenminh.user.dto.UserRequest;
import com.kienlongbank.nguyenminh.user.dto.UserResponse;
import com.kienlongbank.nguyenminh.user.dto.UserResponseLogin;
import com.kienlongbank.nguyenminh.user.handler.JwtSecurityHandler;
import com.kienlongbank.nguyenminh.user.service.UserService;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.SpringBootTest;
import com.kienlongbank.nguyenminh.config.TestJacksonConfig;
import com.kienlongbank.nguyenminh.user.service.impl.UserDubboServiceImpl;
import com.kienlongbank.nguyenminh.UserServiceApplication;
import org.springframework.test.context.ActiveProfiles;
import com.kienlongbank.nguyenminh.config.TestTracerConfig;

import java.util.List;
import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {UserServiceApplication.class, TestJacksonConfig.class, TestTracerConfig.class})
@Import(TestJacksonConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtSecurityHandler jwtSecurityHandler;

    @MockBean
    private MessageSource messageSource;

    @MockBean
    private UserDubboServiceImpl userDubboService;

    private UserRequest sampleRequest;
    private UserResponse sampleResponse;
    private UserResponseLogin loginResponse;

    @BeforeEach
    void setUp() {
        sampleRequest = new UserRequest();
        sampleRequest.setUsername("john");
        sampleRequest.setEmail("john@example.com");

        sampleResponse = new UserResponse();
        sampleResponse.setId(1L);
        sampleResponse.setUsername("john");
        sampleResponse.setEmail("john@example.com");

        loginResponse = new UserResponseLogin();
        loginResponse.setUsername("john");
        loginResponse.setPassword("hashed-password");
    }

    @Test
    void createUser_Success() throws Exception {
        when(userService.createUser(any())).thenReturn(sampleResponse);
        when(messageSource.getMessage(eq("user.create.success"), any(), any(Locale.class)))
                .thenReturn("User created successfully");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User created successfully"))
                .andExpect(jsonPath("$.data.username").value("john"));
    }

    @Test
    void getUserById_Success() throws Exception {
        when(userService.getUserById(1L)).thenReturn(sampleResponse);
        when(messageSource.getMessage(eq("user.get.success"), any(), any(Locale.class)))
                .thenReturn("User retrieved");

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("john"));
    }

    @Test
    void getAllUsers_Success() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(sampleResponse));
        when(messageSource.getMessage(eq("user.list.success"), any(), any(Locale.class)))
                .thenReturn("All users retrieved");

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].username").value("john"));
    }

    @Test
    void updateUser_Success() throws Exception {
        when(userService.updateUser(eq(1L), any())).thenReturn(sampleResponse);
        when(messageSource.getMessage(eq("user.update.success"), any(), any(Locale.class)))
                .thenReturn("User updated");

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("john"));
    }

    @Test
    void deleteUser_Success() throws Exception {
        doNothing().when(userService).deleteUser(1L);
        when(messageSource.getMessage(eq("user.delete.success"), any(), any(Locale.class)))
                .thenReturn("User deleted");

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deleted"));
    }

    @Test
    void findByUsername_Success() throws Exception {
        when(userService.findByUsername("john", "test-token")).thenReturn(loginResponse);

        mockMvc.perform(get("/api/users/name/john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john"));
    }
}
