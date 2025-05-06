package com.kienlongbank.temporal.controller;

import com.kienlongbank.temporal.dto.LoginRequest;
import com.kienlongbank.temporal.dto.LoginResponse;
import com.kienlongbank.temporal.workflow.LoginWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/temporal")
@RequiredArgsConstructor
@Slf4j
public class TemporalLoginController {

    private final WorkflowClient workflowClient;
    
    @Value("${temporal.task-queue:LoginTaskQueue}")
    private String taskQueue;
    
    @Value("${temporal.workflow.execution.timeout:86400}")
    private long workflowExecutionTimeoutSeconds;
    
    @Value("${temporal.workflow.run.retention:259200}")
    private long workflowRunRetentionSeconds;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginWithTemporal(
            @RequestBody LoginRequest request,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            HttpServletRequest httpRequest) {
        
        log.info("Received temporal login request for user: {}, Accept-Language: {}", 
                request.getUserName(), acceptLanguage);
        
        // Store the Accept-Language in the request context
        request.setLocale(acceptLanguage != null ? acceptLanguage : httpRequest.getLocale().toString());
        
        try {
            // Generate a unique workflow ID
            String workflowId = "login-workflow-" + UUID.randomUUID();
            
            // Set workflow options
            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setTaskQueue(taskQueue)
                    .setWorkflowId(workflowId)
                    .setWorkflowExecutionTimeout(Duration.ofSeconds(workflowExecutionTimeoutSeconds))
                    .setWorkflowRunTimeout(Duration.ofSeconds(workflowExecutionTimeoutSeconds))
                    .setWorkflowTaskTimeout(Duration.ofSeconds(10))
                    .build();
            
            // Create the workflow stub
            LoginWorkflow loginWorkflow = workflowClient.newWorkflowStub(LoginWorkflow.class, options);
            
            // Execute the workflow and get the result
            log.info("Starting login workflow with ID: {}", workflowId);
            LoginResponse response = loginWorkflow.login(request);
            
            // Add workflow ID to the response
            response.setWorkflowId(workflowId);
            
            log.info("Completed login workflow for user: {}, success: {}", request.getUserName(), response.isSuccess());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error executing login workflow", e);
            LoginResponse errorResponse = LoginResponse.builder()
                    .success(false)
                    .error("Failed to execute login workflow: " + e.getMessage())
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
} 