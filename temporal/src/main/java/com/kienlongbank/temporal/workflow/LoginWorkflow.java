package com.kienlongbank.temporal.workflow;

import com.kienlongbank.temporal.dto.LoginRequest;
import com.kienlongbank.temporal.dto.LoginResponse;
import com.kienlongbank.temporal.dto.ClassroomResponse;
import com.kienlongbank.temporal.dto.EmailResponse;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import io.temporal.workflow.QueryMethod;

@WorkflowInterface
public interface LoginWorkflow {
    
    /**
     * Main workflow method to process login requests and trigger the complete workflow
     * 1. Login
     * 2. Fetch classrooms
     * 3. Send email notification with classroom data
     * 
     * @param request The login request containing username and password
     * @return LoginResponse with authentication result
     */
    @WorkflowMethod
    LoginResponse login(LoginRequest request);
    
    /**
     * Query method to get available classrooms using the authentication token
     * 
     * @return ClassroomResponse with the list of available classrooms
     */
    @QueryMethod
    ClassroomResponse getClassrooms();
    
    /**
     * Query method to get the status of email notification
     * 
     * @return EmailResponse with the status of the email notification
     */
    @QueryMethod
    EmailResponse getEmailStatus();
} 