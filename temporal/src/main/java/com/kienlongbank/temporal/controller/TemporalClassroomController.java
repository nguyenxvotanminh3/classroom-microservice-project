package com.kienlongbank.temporal.controller;

import com.kienlongbank.temporal.dto.ClassroomResponse;
import com.kienlongbank.temporal.workflow.LoginWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowNotFoundException;
import io.temporal.client.WorkflowQueryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

@RestController
@RequestMapping("/temporal")
@RequiredArgsConstructor
@Slf4j
public class TemporalClassroomController {

    private final WorkflowClient workflowClient;

    @GetMapping("/classrooms/{workflowId}")
    public ResponseEntity<ClassroomResponse> getClassrooms(@PathVariable String workflowId) {
        log.info("Getting classrooms for workflow ID: {}", workflowId);
        
        try {
            // Get workflow stub for query
            LoginWorkflow loginWorkflow = workflowClient.newWorkflowStub(
                    LoginWorkflow.class, 
                    workflowId);
            
            // Query the workflow to get classrooms
            ClassroomResponse response = loginWorkflow.getClassrooms();
            
            // Log the result
            if (response.getData() != null) {
                log.info("Retrieved {} classrooms for workflow ID: {}", 
                        response.getData().size(), workflowId);
            } else {
                log.info("No classrooms retrieved for workflow ID: {}, error: {}", 
                        workflowId, response.getError());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (WorkflowNotFoundException e) {
            // Workflow not found - likely expired or never existed
            log.error("Workflow not found for ID: {}", workflowId, e);
            ClassroomResponse errorResponse = ClassroomResponse.builder()
                    .success(false)
                    .error("Workflow not found. The login session may have expired or the workflow ID is invalid.")
                    .data(Collections.emptyList())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            
        } catch (WorkflowQueryException e) {
            // Query execution failed - likely due to workflow execution issues
            log.error("Error querying workflow for ID: {}", workflowId, e);
            ClassroomResponse errorResponse = ClassroomResponse.builder()
                    .success(false)
                    .error("Error retrieving classroom data. The workflow query failed: " + e.getMessage())
                    .data(Collections.emptyList())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            
        } catch (Exception e) {
            // Unexpected error
            log.error("Unexpected error getting classrooms for workflow ID: {}", workflowId, e);
            ClassroomResponse errorResponse = ClassroomResponse.builder()
                    .success(false)
                    .error("Failed to get classrooms: " + e.getMessage())
                    .data(Collections.emptyList())
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
} 