package com.kienlongbank.emailservice.controller;

import com.kienlongbank.emailservice.dto.UserRegistrationEvent;
import com.kienlongbank.emailservice.service.UserRegistrationConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/emails")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final UserRegistrationConsumer userRegistrationConsumer;

    /**
     * Test endpoint to verify email service is running
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testService() {
        log.info("Test endpoint called");
        return ResponseEntity.ok(Map.of(
            "status", "Email service is running",
            "timestamp", LocalDateTime.now().toString()
        ));
    }
    
    /**
     * Test endpoint to manually process a user registration event
     * This will help verify if the email sending logic works
     */
    @PostMapping("/test-registration")
    public ResponseEntity<Map<String, Object>> testRegistration(@RequestBody UserRegistrationEvent event) {
        log.info("Received manual test event: {}", event);
        
        // Manually create an event if none was provided
        if (event == null) {
            event = UserRegistrationEvent.builder()
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .registrationTime(LocalDateTime.now())
                .build();
            log.info("Created default test event: {}", event);
        }
        
        try {
            // Delegate to the user registration consumer
            log.info("Manual test with event: {}", event);
            userRegistrationConsumer.processUserRegistration(event);
            return ResponseEntity.ok(Map.of(
                "status", "Test event processed successfully",
                "event", event
            ));
        } catch (Exception e) {
            log.error("Error processing test event", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to process test event",
                "message", e.getMessage()
            ));
        }
    }
} 