package com.kienlongbank.temporal.workflow.impl;

import com.kienlongbank.temporal.dto.LoginRequest;
import com.kienlongbank.temporal.dto.LoginResponse;
import com.kienlongbank.temporal.workflow.LoginActivities;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginActivitiesImpl implements LoginActivities {

    private final WebClient securityServiceWebClient;
    private final ObjectMapper objectMapper;

    @Override
    public LoginResponse callLoginApi(LoginRequest request) {
        try {
            log.info("Calling security service API for login: {}", request.getUserName());
            
            // Get locale from request
            String acceptLanguage = request.getLocale();
            log.info("Using Accept-Language: {}", acceptLanguage);
            
            // Gọi API và trả về response trực tiếp từ security-service
            String responseBody = securityServiceWebClient.post()
                    .uri("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.ACCEPT_LANGUAGE, acceptLanguage != null ? acceptLanguage : "vi")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        log.error("Error calling login API: {} - {}", ex.getStatusCode(), ex.getMessage());
                        
                        // Cố gắng đọc response body từ lỗi
                        if (ex.getResponseBodyAsString() != null && !ex.getResponseBodyAsString().isEmpty()) {
                            return Mono.just(ex.getResponseBodyAsString());
                        }
                        
                        // Nếu không có response body, tạo một response lỗi mặc định
                        if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                            try {
                                Map<String, Object> errorMap = new HashMap<>();
                                errorMap.put("token", null);
                                errorMap.put("userName", null);
                                errorMap.put("roles", null);
                                errorMap.put("message", null);
                                errorMap.put("error", "Invalid credentials");
                                errorMap.put("success", false);
                                errorMap.put("email", null);
                                
                                return Mono.just(objectMapper.writeValueAsString(errorMap));
                            } catch (Exception e) {
                                log.error("Error creating error response", e);
                            }
                        }
                        
                        return Mono.just("{\"success\":false,\"error\":\"Error connecting to authentication service\",\"email\":null}");
                    })
                    .block();
            
            log.info("Response from security service: {}", responseBody);
            
            // Parse response từ security-service
            try {
                return objectMapper.readValue(responseBody, LoginResponse.class);
            } catch (Exception e) {
                log.error("Error parsing login response", e);
                return LoginResponse.builder()
                        .success(false)
                        .error("Error parsing response: " + e.getMessage())
                        .email(null)
                        .build();
            }
        } catch (Exception e) {
            log.error("Unexpected error during login API call", e);
            return LoginResponse.builder()
                    .success(false)
                    .error("Unexpected error: " + e.getMessage())
                    .email(null)
                    .build();
        }
    }
} 