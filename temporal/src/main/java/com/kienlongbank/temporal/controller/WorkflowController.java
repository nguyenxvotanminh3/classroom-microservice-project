package com.kienlongbank.temporal.controller;

import com.kienlongbank.temporal.dto.LoginRequest;
import com.kienlongbank.temporal.dto.LoginResponse;
import com.kienlongbank.temporal.dto.ClassroomResponse;
import com.kienlongbank.temporal.dto.EmailResponse;
import com.kienlongbank.temporal.dto.EmailRequest;
import com.kienlongbank.temporal.workflow.LoginWorkflow;
import com.kienlongbank.temporal.workflow.EmailActivities;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
@Slf4j
public class WorkflowController {

    private final WorkflowClient workflowClient;
    private final EmailActivities emailActivities;
    
    @Value("${temporal.task-queue:LoginTaskQueue}")
    private String taskQueue;
    
    @Value("${temporal.workflow.execution.timeout:86400}")
    private long workflowExecutionTimeoutSeconds;

    /**
     * Endpoint to start the complete workflow:
     * 1. Login
     * 2. Fetch classrooms
     * 3. Send email notification
     */
    @PostMapping("/start")
    public ResponseEntity<LoginResponse> startWorkflow(
            @RequestBody LoginRequest request,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            HttpServletRequest httpRequest) {
        
        log.info("Received workflow request for user: {}, Accept-Language: {}", 
                request.getUserName(), acceptLanguage);
        
        // Store the Accept-Language in the request context
        request.setLocale(acceptLanguage != null ? acceptLanguage : httpRequest.getLocale().toString());
        
        try {
            // Generate a unique workflow ID
            String workflowId = "complete-workflow-" + UUID.randomUUID();
            
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
            log.info("Starting complete workflow with ID: {}", workflowId);
            LoginResponse response = loginWorkflow.login(request);
            
            // Add workflow ID to the response
            response.setWorkflowId(workflowId);
            
            log.info("Login phase of workflow completed for user: {}, success: {}", 
                    request.getUserName(), response.isSuccess());
            
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
    
    /**
     * Endpoint to query classroom data from a running workflow
     */
    @GetMapping("/{workflowId}/classrooms")
    public ResponseEntity<ClassroomResponse> getClassrooms(@PathVariable String workflowId) {
        log.info("Received request to query classrooms for workflow ID: {}", workflowId);
        
        try {
            // Get workflow stub
            LoginWorkflow workflow = workflowClient.newWorkflowStub(LoginWorkflow.class, workflowId);
            
            // Query for classroom data
            ClassroomResponse response = workflow.getClassrooms();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error querying classrooms from workflow: {}", workflowId, e);
            
            return ResponseEntity.internalServerError().body(
                ClassroomResponse.builder()
                    .success(false)
                    .error("Failed to query classrooms: " + e.getMessage())
                    .build()
            );
        }
    }
    
    /**
     * Endpoint to query email notification status from a running workflow
     */
    @GetMapping("/{workflowId}/email-status")
    public ResponseEntity<EmailResponse> getEmailStatus(@PathVariable String workflowId) {
        log.info("Received request to query email status for workflow ID: {}", workflowId);
        
        try {
            // Get workflow stub
            LoginWorkflow workflow = workflowClient.newWorkflowStub(LoginWorkflow.class, workflowId);
            
            // Query for email status
            EmailResponse response = workflow.getEmailStatus();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error querying email status from workflow: {}", workflowId, e);
            
            return ResponseEntity.internalServerError().body(
                EmailResponse.builder()
                    .success(false)
                    .error("Failed to query email status: " + e.getMessage())
                    .build()
            );
        }
    }
    
    /**
     * Endpoint to manually send an email with classroom data
     */
    @PostMapping("/send-email")
    public ResponseEntity<EmailResponse> sendEmailManually(@RequestBody EmailRequest request) {
        log.info("Received manual email request to send notification to: {}", request.getTo());
        
        try {
            // Call the email activity directly
            EmailResponse response = emailActivities.sendEmail(request);
            
            log.info("Email sending result: success={}, error={}", 
                response.isSuccess(), response.getError());
                
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error sending email manually: {}", e.getMessage(), e);
            EmailResponse errorResponse = EmailResponse.builder()
                    .success(false)
                    .error("Failed to send email: " + e.getMessage())
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
} 