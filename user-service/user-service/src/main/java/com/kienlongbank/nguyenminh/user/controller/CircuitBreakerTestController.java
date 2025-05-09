package com.kienlongbank.nguyenminh.user.controller;

import com.kienlongbank.nguyenminh.user.dto.UserRequest;
import com.kienlongbank.nguyenminh.user.dto.UserResponse;
import com.kienlongbank.nguyenminh.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/circuit-breaker-test")
@RequiredArgsConstructor
@Slf4j
public class CircuitBreakerTestController {

    private final UserService userService;
    private final AtomicInteger successCounter = new AtomicInteger(0);
    private final AtomicInteger failureCounter = new AtomicInteger(0);

    /**
     * Stress test endpoint to trigger circuit breaker by sending many concurrent user creation requests
     * This will help demonstrate how the circuit breaker works in createUser method
     */
    @PostMapping("/stress-test")
    public ResponseEntity<?> stressTest(@RequestParam(defaultValue = "20") int concurrentRequests,
                                         @RequestParam(defaultValue = "false") boolean includeDuplicates) {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        // Reset counters
        successCounter.set(0);
        failureCounter.set(0);
        
        log.info("Starting stress test with {} concurrent requests, includeDuplicates: {}", 
                concurrentRequests, includeDuplicates);
        
        // Create a template user to duplicate if needed
        UserRequest template;
        if (includeDuplicates) {
            template = createRandomUserRequest();
            // First request should succeed
            try {
                userService.createUser(template);
                successCounter.incrementAndGet();
                log.info("Created template user: {}", template.getUsername());
            } catch (Exception e) {
                failureCounter.incrementAndGet();
                log.error("Failed to create template user: {}", e.getMessage());
            }
        } else {
            template = null;
        }

        // Launch concurrent requests
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestNumber = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    UserRequest userRequest;
                    
                    // Either use duplicate user data or generate random user
                    if (includeDuplicates && requestNumber % 2 == 0) {
                        // Create duplicate to force failure every second request
                        userRequest = new UserRequest(
                                template.getUsername(),
                                template.getFullName(),
                                template.getEmail(),
                                template.getPassword()
                        );
                    } else {
                        userRequest = createRandomUserRequest();
                    }
                    
                    log.info("Sending request #{}: {}", requestNumber, userRequest.getUsername());
                    UserResponse response = userService.createUser(userRequest);
                    log.info("Request #{} succeeded: {}", requestNumber, response.getUsername());
                    successCounter.incrementAndGet();
                } catch (Exception e) {
                    log.error("Request #{} failed: {}", requestNumber, e.getMessage());
                    failureCounter.incrementAndGet();
                }
            }, executorService);
            
            futures.add(future);
        }
        
        // Wait for all futures to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executorService.shutdown();
        
        // Return results
        Map<String, Object> result = Map.of(
                "totalRequests", concurrentRequests + (includeDuplicates ? 1 : 0),
                "successful", successCounter.get(),
                "failed", failureCounter.get(),
                "circuitBreakerTriggered", failureCounter.get() > 0
        );
        
        log.info("Stress test completed: {}", result);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Create a user with forced delay to trigger timeout and open circuit breaker
     */
    @PostMapping("/slow-requests")
    public ResponseEntity<?> createSlowUser(@RequestParam(defaultValue = "5") int delaySeconds) {
        UserRequest userRequest = createRandomUserRequest();
        
        try {
            // Simulate slow processing before actual user creation
            log.info("Creating user with deliberate delay of {} seconds: {}", 
                    delaySeconds, userRequest.getUsername());
            Thread.sleep(delaySeconds * 1000);
            
            UserResponse response = userService.createUser(userRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating user with delay: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Reset test counters
     */
    @PostMapping("/reset-counters")
    public ResponseEntity<?> resetCounters() {
        successCounter.set(0);
        failureCounter.set(0);
        return ResponseEntity.ok(Map.of(
                "successCounter", successCounter.get(),
                "failureCounter", failureCounter.get()
        ));
    }
    
    /**
     * Helper method to create random user requests
     */
    private UserRequest createRandomUserRequest() {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return new UserRequest(
                "user_" + uuid,
                "Test User " + uuid,
                "test_" + uuid + "@example.com",
                "Password1"
        );
    }
} 