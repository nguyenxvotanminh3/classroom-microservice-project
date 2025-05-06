package com.kienlongbank.nguyenminh.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.kienlongbank.nguyenminh.user.dto.UserRequest;
import com.kienlongbank.nguyenminh.user.dto.UserResponse;
import com.kienlongbank.nguyenminh.user.event.UserRegistrationEvent;
import com.kienlongbank.nguyenminh.UserServiceApplication;
import com.kienlongbank.nguyenminh.config.TestDubboMockConfig;
import com.kienlongbank.nguyenminh.config.TestJacksonConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {UserServiceApplication.class, TestDubboMockConfig.class, TestJacksonConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test") // Sử dụng profile test
public class UserServiceWireMockTest {

    private WireMockServer wireMockServer;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        // Khởi tạo WireMock server trên port 8089
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);

        // Giả lập endpoint cho Email Service
        stubEmailServiceEndpoints();
        
        // Giả lập endpoint cho Security Service
        stubSecurityServiceEndpoints();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    // Test tạo user thành công và gửi sự kiện đăng ký
    @Test
    void createUser_Success_SendsEmailNotification() throws Exception {
        // Chuẩn bị dữ liệu test
        UserRequest userRequest = new UserRequest();
        userRequest.setUsername("newuser");
        userRequest.setFullName("New User");
        userRequest.setEmail("new@example.com");
        userRequest.setPassword("password123");

        // Gọi API để tạo user
        ResultActions resultActions = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)));

        // Kiểm tra kết quả
        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data.username").value("newuser"))
                .andExpect(jsonPath("$.data.email").value("new@example.com"));

        // Xác minh rằng email thông báo đã được gửi
        verify(postRequestedFor(urlPathEqualTo("/api/emails/notification"))
                .withRequestBody(containing("newuser")));
    }

    // Test lỗi username đã tồn tại
    @Test
    void createUser_UsernameExists_ReturnsConflict() throws Exception {
        // Chuẩn bị dữ liệu test - username đã tồn tại
        UserRequest userRequest = new UserRequest();
        userRequest.setUsername("existinguser");
        userRequest.setFullName("Existing User");
        userRequest.setEmail("new@example.com");
        userRequest.setPassword("password123");

        // Giả lập khi repository.existsByUsername trả về true
        // (Yêu cầu cấu hình đặc biệt trong application-test.properties
        // hoặc sửa đổi code để hỗ trợ test case này)

        // Gọi API để tạo user
        ResultActions resultActions = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)));

        // Kiểm tra kết quả - mong đợi lỗi conflict
        resultActions
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(containsString("Username 'existinguser' already exists")));

        // Xác minh rằng KHÔNG có email thông báo nào được gửi
        verify(0, postRequestedFor(urlPathEqualTo("/api/emails/notification")));
    }

    // Test lỗi email đã tồn tại
    @Test
    void createUser_EmailExists_ReturnsConflict() throws Exception {
        // Chuẩn bị dữ liệu test - email đã tồn tại
        UserRequest userRequest = new UserRequest();
        userRequest.setUsername("newuser");
        userRequest.setFullName("New User");
        userRequest.setEmail("existing@example.com");
        userRequest.setPassword("password123");

        // Giả lập khi repository.existsByEmail trả về true
        // (Yêu cầu cấu hình đặc biệt trong application-test.properties
        // hoặc sửa đổi code để hỗ trợ test case này)

        // Gọi API để tạo user
        ResultActions resultActions = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)));

        // Kiểm tra kết quả - mong đợi lỗi conflict
        resultActions
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(containsString("Email 'existing@example.com' already exists")));

        // Xác minh rằng KHÔNG có email thông báo nào được gửi
        verify(0, postRequestedFor(urlPathEqualTo("/api/emails/notification")));
    }

    // Test khi Email Service gặp lỗi
    @Test
    void createUser_EmailServiceFails_StillCreatesUser() throws Exception {
        // Thay đổi giả lập cho Email Service để trả về lỗi 500
        stubFor(WireMock.post(urlPathEqualTo("/api/emails/notification"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Internal Server Error\"}")));

        // Chuẩn bị dữ liệu test
        UserRequest userRequest = new UserRequest();
        userRequest.setUsername("user_with_email_error");
        userRequest.setFullName("Email Error User");
        userRequest.setEmail("email_error@example.com");
        userRequest.setPassword("password123");

        // Gọi API để tạo user
        ResultActions resultActions = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)));

        // Kiểm tra kết quả - user vẫn được tạo mặc dù email service lỗi
        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data.username").value("user_with_email_error"));

        // Xác minh rằng hệ thống đã cố gắng gửi thông báo
        verify(postRequestedFor(urlPathEqualTo("/api/emails/notification")));
    }

    // Phương thức giả lập endpoints cho Email Service
    private void stubEmailServiceEndpoints() {
        // Giả lập endpoint gửi email thông báo
        stubFor(WireMock.post(urlPathEqualTo("/api/emails/notification"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"Email sent successfully\"}")));

        // Giả lập endpoint xác minh email
        stubFor(WireMock.post(urlPathEqualTo("/api/emails/verification"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"Verification email sent successfully\"}")));
    }

    // Phương thức giả lập endpoints cho Security Service
    private void stubSecurityServiceEndpoints() {
        // Giả lập endpoint xác thực token
        stubFor(WireMock.post(urlPathEqualTo("/api/auth/validate"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"valid\": true, \"username\": \"testuser\"}")));
        
        // Giả lập endpoint đăng ký
        stubFor(WireMock.post(urlPathEqualTo("/api/auth/register"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"User registered successfully\"}")));
    }
} 