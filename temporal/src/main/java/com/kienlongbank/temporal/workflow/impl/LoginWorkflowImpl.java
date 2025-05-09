package com.kienlongbank.temporal.workflow.impl;

import com.kienlongbank.temporal.dto.LoginRequest;
import com.kienlongbank.temporal.dto.LoginResponse;
import com.kienlongbank.temporal.dto.ClassroomResponse;
import com.kienlongbank.temporal.dto.EmailRequest;
import com.kienlongbank.temporal.dto.EmailResponse;
import com.kienlongbank.temporal.dto.ClassroomDto;
import com.kienlongbank.temporal.workflow.LoginActivities;
import com.kienlongbank.temporal.workflow.ClassroomActivities;
import com.kienlongbank.temporal.workflow.EmailActivities;
import com.kienlongbank.temporal.workflow.LoginWorkflow;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Collections;
import java.util.stream.Collectors;

@Slf4j
public class LoginWorkflowImpl implements LoginWorkflow {

    // Define activity retry options
    private final RetryOptions retryOptions = RetryOptions.newBuilder()
            .setInitialInterval(Duration.ofSeconds(1))
            .setMaximumInterval(Duration.ofSeconds(10))
            .setBackoffCoefficient(2.0)
            .setMaximumAttempts(5)
            .build();

    // Define activity options with timeouts and retry policy
    private final ActivityOptions activityOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .setRetryOptions(retryOptions)
            .build();

    // Create an activity stub
    private final LoginActivities loginActivities = 
            Workflow.newActivityStub(LoginActivities.class, activityOptions);
            
    private final ClassroomActivities classroomActivities = 
            Workflow.newActivityStub(ClassroomActivities.class, activityOptions);
    
    private final EmailActivities emailActivities =
            Workflow.newActivityStub(EmailActivities.class, activityOptions);
            
    // Store login request and response for later use
    private LoginRequest loginRequest;
    private LoginResponse loginResponse;
    // Store classroom response for query
    private ClassroomResponse cachedClassroomResponse;
    // Store email response for query
    private EmailResponse cachedEmailResponse;
    // Store workflow ID
    private String workflowId;

    @Override
    public LoginResponse login(LoginRequest request) {
        // Store the login request for later use
        this.loginRequest = request;
        
        // Lưu trữ workflow ID
        this.workflowId = Workflow.getInfo().getWorkflowId();
        
        // Log the start of workflow execution
        Workflow.getLogger(this.getClass()).info("Started login workflow for user: " + request.getUserName() + " with workflowId: " + workflowId);
        
        try {
            // Execute the login activity
            LoginResponse response = loginActivities.callLoginApi(request);
            
            // Store the login response for later use
            this.loginResponse = response;
            
            // Đảm bảo workflowId được truyền qua trong response
            response.setWorkflowId(this.workflowId);
            
            Workflow.getLogger(this.getClass()).info("Login response details - email: " + response.getEmail() + ", token: " + (response.getToken() != null ? "present" : "missing"));
            
            if (response.isSuccess()) {
                Workflow.getLogger(this.getClass()).info("Login successful for user: " + request.getUserName());
                
                // If login is successful, fetch the classroom data
                this.cachedClassroomResponse = fetchClassroomData();
                

                if (this.cachedClassroomResponse != null && this.cachedClassroomResponse.getData() != null) {
                    Workflow.getLogger(this.getClass()).info("Classroom data found, attempting to send email notification now...");
                    this.cachedEmailResponse = sendEmailNotification();
                    Workflow.getLogger(this.getClass()).info("Email notification attempt completed. Success: {}", 
                        this.cachedEmailResponse != null ? this.cachedEmailResponse.isSuccess() : "null");
                } else {
                    Workflow.getLogger(this.getClass()).error("Cannot send email because classroom data is null");
                }
            } else {
                Workflow.getLogger(this.getClass()).info("Login failed for user: " + request.getUserName() + " - " + response.getError());
            }
            
            return response;
        } catch (Exception e) {
            // Handle unexpected exceptions (activity retries are handled by Temporal)
            Workflow.getLogger(this.getClass()).error("Workflow execution failed: " + e.getMessage());
            LoginResponse errorResponse = LoginResponse.builder()
                    .success(false)
                    .error("Workflow execution failed: " + e.getMessage())
                    .build();
            errorResponse.setWorkflowId(this.workflowId);
            return errorResponse;
        }
    }
    
    // Method to fetch classroom data
    private ClassroomResponse fetchClassroomData() {
        if (this.loginResponse != null && this.loginResponse.isSuccess() && this.loginResponse.getToken() != null) {
            try {
                // Get the token from login response
                String token = this.loginResponse.getToken();
                
                // Get locale from the stored login request
                String locale = this.loginRequest != null ? this.loginRequest.getLocale() : null;
                
                // Log the classroom API call
                Workflow.getLogger(this.getClass()).info("Fetching classrooms for user: " + loginResponse.getUserName() + " with workflowId: " + workflowId);
                
                // Call the classroom activity and return the result
                ClassroomResponse classroomResponse = classroomActivities.getClassrooms(token, locale);
                
                // Log the result
                if (classroomResponse.getData() != null) {
                    Workflow.getLogger(this.getClass()).info("Successfully fetched {} classrooms", 
                        classroomResponse.getData() != null ? 
                        classroomResponse.getData().size() : 0);
                    // Đảm bảo success là true nếu có dữ liệu
                    if (!classroomResponse.getData().isEmpty()) {
                        classroomResponse.setSuccess(true);
                    }
                } else if (classroomResponse.getError() != null) {
                    Workflow.getLogger(this.getClass()).info("Failed to fetch classrooms: {}", 
                        classroomResponse.getError());
                } else {
                    Workflow.getLogger(this.getClass()).info("No classroom data returned or unknown error");
                }
                
                return classroomResponse;
            } catch (Exception e) {
                // Handle unexpected exceptions
                Workflow.getLogger(this.getClass()).error("Error fetching classrooms: " + e.getMessage());
                return ClassroomResponse.builder()
                        .success(false)
                        .error("Error fetching classrooms: " + e.getMessage())
                        .data(Collections.emptyList())
                        .build();
            }
        }
        
        return ClassroomResponse.builder()
                .success(false)
                .error("No valid authentication token available")
                .data(Collections.emptyList())
                .build();
    }
    
    // Method to send email notification with classroom data
    private EmailResponse sendEmailNotification() {
        if (this.loginResponse != null && this.loginResponse.isSuccess() && 
            this.cachedClassroomResponse != null && this.cachedClassroomResponse.getData() != null) {
            try {
                // Get the token and email from login response
                String token = this.loginResponse.getToken();
                String email = this.loginResponse.getEmail();
                
                Workflow.getLogger(this.getClass()).info("Processing email notification. Email from login response: " + email);
                
                if (email == null || email.isEmpty()) {
                    // Fallback to using username if email is not available
                    email = this.loginRequest.getUserName();
                    // Add domain if it looks like just a username
                    if (!email.contains("@")) {
                        email = email + "@example.com";
                    }
                    Workflow.getLogger(this.getClass()).info("Using fallback email: " + email);
                }
                
                // Create message from classroom data
                String message = formatClassroomMessage(this.cachedClassroomResponse);
                Workflow.getLogger(this.getClass()).info("Generated email message with length: " + message.length());
                
                // Create email request
                EmailRequest emailRequest = EmailRequest.builder()
                        .to(email)
                        .token(token)
                        .message(message)
                        .build();
                
                // Log the email API call
                Workflow.getLogger(this.getClass()).info("Sending email notification to: {}", email);
                
                try {
                    // Call the email activity and return the result
                    EmailResponse emailResponse = emailActivities.sendEmail(emailRequest);
                    
                    // Log the result - kiểm tra cả status và success
                    String successStatus = emailResponse.isSuccess() ? "success=true" : 
                                          (emailResponse.getStatus() != null ? "status=" + emailResponse.getStatus() : "no success info");
                                          
                    if (emailResponse.isSuccess() || 
                        (emailResponse.getStatus() != null && emailResponse.getStatus().equalsIgnoreCase("SUCCESS"))) {
                        Workflow.getLogger(this.getClass()).info("Successfully sent email notification to: {}, {}", 
                            email, successStatus);
                            
                        // Đảm bảo đặt success = true nếu status là SUCCESS
                        if (!emailResponse.isSuccess() && 
                            emailResponse.getStatus() != null && 
                            emailResponse.getStatus().equalsIgnoreCase("SUCCESS")) {
                            // Tạo response mới với success=true
                            emailResponse = EmailResponse.builder()
                                .success(true)
                                .status(emailResponse.getStatus())
                                .message(emailResponse.getMessage())
                                .build();
                        }
                    } else {
                        Workflow.getLogger(this.getClass()).info("Failed to send email notification: {}, {}", 
                            emailResponse.getError(), successStatus);
                    }
                    
                    return emailResponse;
                } catch (Exception e) {
                    Workflow.getLogger(this.getClass()).error("Exception calling email activity: " + e.getMessage(), e);
                    throw e;
                }
            } catch (Exception e) {
                // Handle unexpected exceptions
                Workflow.getLogger(this.getClass()).error("Error sending email notification: " + e.getMessage());
                return EmailResponse.builder()
                        .success(false)
                        .error("Error sending email notification: " + e.getMessage())
                        .build();
            }
        }
        
        Workflow.getLogger(this.getClass()).warn("Cannot send email: loginResponse success={}, classroom data not null={}", 
            (this.loginResponse != null ? this.loginResponse.isSuccess() : "null"),
            (this.cachedClassroomResponse != null && this.cachedClassroomResponse.getData() != null));
            
        return EmailResponse.builder()
                .success(false)
                .error("Cannot send email: No valid authentication or classroom data available")
                .build();
    }
    
    // Helper method to format classroom data as a message
    private String formatClassroomMessage(ClassroomResponse classroomResponse) {
        if (classroomResponse == null || classroomResponse.getData() == null || classroomResponse.getData().isEmpty()) {
            return "No classrooms found.";
        }
        
        StringBuilder message = new StringBuilder("Your Classrooms:\n\n");
        
        for (ClassroomDto classroom : classroomResponse.getData()) {
            message.append("- ").append(classroom.getName())
                  .append(" (").append(classroom.getId()).append(")")
                  .append("\n");
        }
        
        return message.toString();
    }
    
    @Override
    public ClassroomResponse getClassrooms() {
        // Return the cached response if available
        if (this.cachedClassroomResponse != null) {
            return this.cachedClassroomResponse;
        }
        
        // If we don't have cached data but login was successful
        if (this.loginResponse != null && this.loginResponse.isSuccess()) {
            return ClassroomResponse.builder()
                    .success(false)
                    .error("Classroom data not yet available. Please try again.")
                    .data(Collections.emptyList())
                    .build();
        }
        
        // If login wasn't successful
        return ClassroomResponse.builder()
                .success(false)
                .error("No authentication token available. Please login first.")
                .data(Collections.emptyList())
                .build();
    }
    
    @Override
    public EmailResponse getEmailStatus() {
        // Return the cached response if available
        if (this.cachedEmailResponse != null) {
            return this.cachedEmailResponse;
        }
        
        // If email hasn't been sent yet
        return EmailResponse.builder()
                .success(false)
                .error("Email notification not yet sent. Please try again.")
                .build();
    }
} 