package com.kienlongbank.temporal.workflow.impl;

import com.kienlongbank.temporal.dto.ClassroomResponse;
import com.kienlongbank.temporal.workflow.ClassroomActivities;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassroomActivitiesImpl implements ClassroomActivities {

    private final WebClient classroomServiceWebClient;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public ClassroomResponse getClassrooms(String token, String locale) {
        try {
            log.info("Calling classroom service API to get available classrooms");
            
            // Extract the actual token from "Bearer <token>"
            String authToken = token;
            if (token != null && token.startsWith("Bearer ")) {
                // Token already includes "Bearer " prefix, use as is
                authToken = token;
            } else if (token != null) {
                // Token doesn't have the "Bearer " prefix, add it
                authToken = "Bearer " + token;
            }
            
            // Extract workflowId from token if available
            String workflowId = "default-workflow";
            try {
                // Nếu token là JWT, có thể parse để lấy workflowId
                // Hoặc sử dụng workflowId default nếu không thể lấy
                log.info("Using workflowId: {}", workflowId);
            } catch (Exception e) {
                log.warn("Could not extract workflowId from token, using default");
            }
            
            log.info("Using Accept-Language: {}", locale);
            
            // Call the classroom API with the token from login
            String responseBody = classroomServiceWebClient.get()
                    .uri("/api/classrooms")
                    .header(HttpHeaders.AUTHORIZATION, authToken)
                    .header(HttpHeaders.ACCEPT_LANGUAGE, locale != null ? locale : "vi")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        log.error("Error calling classroom API: {} - {}", ex.getStatusCode(), ex.getMessage());
                        
                        // Try to read response body from error
                        ex.getResponseBodyAsString();
                        if (!ex.getResponseBodyAsString().isEmpty()) {
                            return Mono.just(ex.getResponseBodyAsString());
                        }
                        
                        // If there's no response body, create a default error response
                        try {
                            return Mono.just(objectMapper.writeValueAsString(
                                ClassroomResponse.builder()
                                    .success(false)
                                    .error("Error calling classroom API: " + ex.getMessage())
                                    .data(Collections.emptyList())
                                    .build()
                            ));
                        } catch (Exception e) {
                            log.error("Error creating error response", e);
                            return Mono.just("{\"success\":false,\"error\":\"Error calling classroom API\",\"data\":[]}");
                        }
                    })
                    .block();
            
            log.info("Response from classroom service: {}", responseBody);
            
            // Parse response from classroom service
            try {
                ClassroomResponse response = objectMapper.readValue(responseBody, ClassroomResponse.class);
                
                // ĐỂ ĐẢM BẢO THÀNH CÔNG NẾU CÓ DỮ LIỆU
                if (response.getData() != null && !response.getData().isEmpty()) {
                    response.setSuccess(true);
                    log.info("Found data, setting success=true");
                } else if (response.getData() == null && response.getError() == null) {
                    response.setSuccess(true);
                    response.setData(Collections.emptyList());
                    log.info("No data found but no error, setting success=true with empty list");
                }
                
                log.info("ClassroomResponse success: {}, data size: {}", 
                    response.isSuccess(), 
                    response.getData() != null ? response.getData().size() : 0);
                    
                return response;
            } catch (Exception e) {
                log.error("Error parsing classroom response: {}", e.getMessage());
                return ClassroomResponse.builder()
                        .success(false)
                        .error("Error parsing response: " + e.getMessage())
                        .data(Collections.emptyList())
                        .build();
            }
        } catch (Exception e) {
            log.error("Unexpected error during classroom API call", e);
            return ClassroomResponse.builder()
                    .success(false)
                    .error("Unexpected error: " + e.getMessage())
                    .data(Collections.emptyList())
                    .build();
        }
    }
} 