package com.kienlongbank.emailservice.controller;

import com.kienlongbank.emailservice.dto.EmailRequest;
import com.kienlongbank.emailservice.service.EmailService;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/emails")
@RequiredArgsConstructor
@Slf4j
public class EmailController {

    private final EmailService emailService;
    private final Tracer tracer;

    @PostMapping("/reset-password")
    public ResponseEntity<?> sendPasswordResetEmail(@Valid @RequestBody EmailRequest emailRequest) {
        Span span = tracer.nextSpan().name("sendPasswordResetEmail").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            span.tag("email.to", emailRequest.getTo());
            log.info("Received request to send password reset email to: {}", emailRequest.getTo());
            
            emailService.sendEmail(emailRequest.getTo(), emailRequest.getToken());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Password reset email sent successfully");
            response.put("status", "SUCCESS");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            span.tag("error", e.getMessage());
            log.error("Error sending password reset email: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Failed to send password reset email");
            response.put("error", e.getMessage());
            response.put("status", "FAILED");
            
            return ResponseEntity.badRequest().body(response);
        } finally {
            span.end();
        }
    }

    @PostMapping("/verification")
    public ResponseEntity<?> sendVerificationEmail(@Valid @RequestBody EmailRequest emailRequest) {
        Span span = tracer.nextSpan().name("sendVerificationEmail").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            span.tag("email.to", emailRequest.getTo());
            log.info("Received request to send verification email to: {}", emailRequest.getTo());
            
            emailService.sendVerificationEmail(emailRequest.getTo(), emailRequest.getToken());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Verification email sent successfully");
            response.put("status", "SUCCESS");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            span.tag("error", e.getMessage());
            log.error("Error sending verification email: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Failed to send verification email");
            response.put("error", e.getMessage());
            response.put("status", "FAILED");
            
            return ResponseEntity.badRequest().body(response);
        } finally {
            span.end();
        }
    }
    
    @PostMapping("/notification")
    public ResponseEntity<?> sendNotificationEmail(@Valid @RequestBody EmailRequest emailRequest) {
        Span span = tracer.nextSpan().name("sendNotificationEmail").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            span.tag("email.to", emailRequest.getTo());
            log.info("Received request to send notification email to: {}", emailRequest.getTo());
            
            emailService.sendNotificationEmail(emailRequest.getTo(), emailRequest.getMessageContent());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Notification email sent successfully");
            response.put("status", "SUCCESS");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            span.tag("error", e.getMessage());
            log.error("Error sending notification email: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Failed to send notification email");
            response.put("error", e.getMessage());
            response.put("status", "FAILED");
            
            return ResponseEntity.badRequest().body(response);
        } finally {
            span.end();
        }
    }
} 