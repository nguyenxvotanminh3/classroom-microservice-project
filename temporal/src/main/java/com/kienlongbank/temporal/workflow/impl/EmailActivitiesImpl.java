package com.kienlongbank.temporal.workflow.impl;

import com.kienlongbank.temporal.dto.EmailRequest;
import com.kienlongbank.temporal.dto.EmailResponse;
import com.kienlongbank.temporal.workflow.EmailActivities;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailActivitiesImpl implements EmailActivities {

    private final WebClient emailServiceWebClient;
    private final ObjectMapper objectMapper;

    @Override
    public EmailResponse sendEmail(EmailRequest request) {
        try {
            log.info("Calling email service API to send notification to: {}", request.getTo());
            log.info("Email message content: {}", request.getMessage());
            
            // Call the email API
            String responseBody = emailServiceWebClient.post()
                    .uri("/api/emails/notification")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        log.error("Error calling email API: {} - {}", ex.getStatusCode(), ex.getMessage());
                        log.error("Error response body: {}", ex.getResponseBodyAsString());
                        
                        // Try to read response body from error
                        if (ex.getResponseBodyAsString() != null && !ex.getResponseBodyAsString().isEmpty()) {
                            return Mono.just(ex.getResponseBodyAsString());
                        }
                        
                        // If there's no response body, create a default error response
                        return Mono.just("{\"success\":false,\"error\":\"Error sending email: " + ex.getMessage() + "\"}");
                    })
                    .doOnError(e -> {
                        if (!(e instanceof WebClientResponseException)) {
                            log.error("Unexpected error in WebClient call: ", e);
                        }
                    })
                    .block();
            
            log.info("Response from email service: {}", responseBody);
            
            // PHƯƠNG PHÁP MỚI: Không sử dụng Jackson để parse trực tiếp thành EmailResponse
            // Mà kiểm tra response và tự tạo EmailResponse
            if (responseBody != null) {
                // Nếu response chứa "SUCCESS" hoặc "successfully" -> success
                boolean isSuccess = responseBody.contains("SUCCESS") || 
                                    responseBody.contains("successfully") ||
                                    responseBody.contains("success");
                
                // Nếu response chứa "error" hoặc "ERROR" -> error message
                String errorMsg = null;
                if (responseBody.contains("error\":")) {
                    try {
                        JsonNode node = objectMapper.readTree(responseBody);
                        if (node.has("error")) {
                            errorMsg = node.get("error").asText();
                        }
                    } catch (Exception e) {
                        log.warn("Could not extract error from JSON: {}", e.getMessage());
                    }
                }
                
                // Nếu response chứa "message" -> message
                String message = null;
                if (responseBody.contains("message\":")) {
                    try {
                        JsonNode node = objectMapper.readTree(responseBody);
                        if (node.has("message")) {
                            message = node.get("message").asText();
                        }
                    } catch (Exception e) {
                        log.warn("Could not extract message from JSON: {}", e.getMessage());
                    }
                }
                
                // Nếu response chứa "status" -> status
                String status = null;
                if (responseBody.contains("status\":")) {
                    try {
                        JsonNode node = objectMapper.readTree(responseBody);
                        if (node.has("status")) {
                            status = node.get("status").asText();
                            // Nếu status là SUCCESS -> success = true
                            if ("SUCCESS".equalsIgnoreCase(status)) {
                                isSuccess = true;
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Could not extract status from JSON: {}", e.getMessage());
                    }
                }
                
                // Tạo EmailResponse mới
                EmailResponse response = EmailResponse.builder()
                        .success(isSuccess)
                        .error(errorMsg)
                        .message(message)
                        .status(status)
                        .build();
                
                log.info("Created EmailResponse: success={}, status={}, message={}, error={}", 
                        response.isSuccess(), 
                        response.getStatus(), 
                        response.getMessage(), 
                        response.getError());
                        
                return response;
            }
            
            // Fallback nếu có lỗi
            return EmailResponse.builder()
                    .success(false)
                    .error("Could not process email service response")
                    .build();
        } catch (Exception e) {
            log.error("Unexpected error during email API call", e);
            e.printStackTrace(); // In ra stack trace đầy đủ
            return EmailResponse.builder()
                    .success(false)
                    .error("Unexpected error: " + e.getMessage())
                    .build();
        }
    }
} 