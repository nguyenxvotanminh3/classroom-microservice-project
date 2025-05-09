package com.kienlongbank.nguyenminh.user.controller;

import com.kienlongbank.nguyenminh.user.dto.UserRequest;
import com.kienlongbank.nguyenminh.user.dto.UserResponse;
import com.kienlongbank.nguyenminh.user.repository.UserRepository;
import com.kienlongbank.nguyenminh.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/circuit-breaker-load")
@RequiredArgsConstructor
@Slf4j
public class CircuitBreakerLoadController {

    private final UserService userService;
    private final UserRepository userRepository;
    
    // Memory leak simulation - holds references to prevent garbage collection
    private static final Map<String, List<byte[]>> memoryLeakMap = new ConcurrentHashMap<>();
    
    // Used to track if a memory pressure test is active
    private final AtomicBoolean memoryTestActive = new AtomicBoolean(false);
    
    // Scheduled executor for cleaning up memory after test
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    /**
     * Endpoint to simulate database connection pool exhaustion by creating many
     * database operations in parallel that don't release connections quickly
     */
    @PostMapping("/connection-pool")
    public ResponseEntity<?> simulateConnectionPoolExhaustion(
            @RequestParam(defaultValue = "50") int requests,
            @RequestParam(defaultValue = "500") int sleepMillis) {
        
        log.info("Starting connection pool exhaustion simulation with {} requests and {}ms sleep",
                requests, sleepMillis);
        
        List<Thread> threads = new ArrayList<>();
        Instant start = Instant.now();
        
        // Create multiple threads that will each get a database connection and hold it
        for (int i = 0; i < requests; i++) {
            Thread t = new Thread(() -> {
                try {
                    // Do a query that obtains a database connection
                    log.info("Thread executing database operation");
                    userRepository.findAll();
                    
                    // Simulate slow processing which keeps the connection open
                    Thread.sleep(sleepMillis);
                    
                    // Create a user - this operation requires a transaction and connection
                    UserRequest userRequest = createRandomUserRequest();
                    userService.createUser(userRequest);
                    
                } catch (Exception e) {
                    log.error("Error in connection pool test thread: {}", e.getMessage());
                }
            });
            
            threads.add(t);
            t.start();
        }
        
        // Wait for all threads to complete
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                log.error("Thread join interrupted", e);
            }
        }
        
        Instant end = Instant.now();
        long totalTimeMs = Duration.between(start, end).toMillis();
        
        Map<String, Object> result = Map.of(
                "totalRequests", requests,
                "sleepMillisPerRequest", sleepMillis,
                "totalExecutionTimeMs", totalTimeMs,
                "message", "Connection pool exhaustion simulation completed"
        );
        
        log.info("Connection pool exhaustion simulation completed in {}ms", totalTimeMs);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Endpoint to simulate memory pressure which can affect service performance
     * and trigger circuit breaker
     */
    @PostMapping("/memory-pressure")
    public ResponseEntity<?> simulateMemoryPressure(
            @RequestParam(defaultValue = "100") int mbToAllocate,
            @RequestParam(defaultValue = "60") int holdForSeconds) {
        
        if (memoryTestActive.get()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Memory pressure test already active",
                    "message", "Wait for current test to complete or call /release-memory"
            ));
        }
        
        memoryTestActive.set(true);
        
        log.info("Starting memory pressure simulation, allocating {}MB for {} seconds", 
                mbToAllocate, holdForSeconds);
        
        // Generate a unique key for this test
        String testKey = UUID.randomUUID().toString();
        List<byte[]> memoryBlocks = new ArrayList<>();
        
        // Allocate memory in 1 MB chunks
        for (int i = 0; i < mbToAllocate && memoryTestActive.get(); i++) {
            try {
                // Allocate 1 MB
                byte[] block = new byte[1024 * 1024];
                // Fill with random data to ensure it's allocated
                new Random().nextBytes(block);
                memoryBlocks.add(block);
                
                if (i % 10 == 0) {
                    log.info("Allocated {}MB of memory", i);
                }
                
                // Create a user through the service to trigger potential circuit breaker
                if (i % 5 == 0) {
                    UserRequest userRequest = createRandomUserRequest();
                    try {
                        userService.createUser(userRequest);
                        log.info("Created user during memory pressure: {}", userRequest.getUsername());
                    } catch (Exception e) {
                        log.error("Error creating user during memory pressure: {}", e.getMessage());
                    }
                }
                
                // Small sleep to allow for interruption
                Thread.sleep(10);
                
            } catch (OutOfMemoryError e) {
                log.error("Out of memory error during simulation: {}", e.getMessage());
                break;
            } catch (InterruptedException e) {
                log.error("Memory allocation interrupted", e);
                break;
            }
        }
        
        // Store the blocks in the map
        memoryLeakMap.put(testKey, memoryBlocks);
        
        // Schedule cleanup after the hold period
        scheduler.schedule(() -> {
            releaseMemory(testKey);
        }, holdForSeconds, TimeUnit.SECONDS);
        
        Map<String, Object> result = Map.of(
                "testKey", testKey,
                "allocatedMB", memoryBlocks.size(),
                "holdForSeconds", holdForSeconds,
                "message", "Memory pressure simulation active"
        );
        
        log.info("Memory pressure simulation started with key: {}", testKey);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Release memory allocated by a specific test
     */
    @PostMapping("/release-memory/{testKey}")
    public ResponseEntity<?> releaseMemoryByKey(@PathVariable String testKey) {
        boolean released = releaseMemory(testKey);
        return ResponseEntity.ok(Map.of(
                "testKey", testKey,
                "released", released,
                "message", released ? "Memory released" : "Test key not found"
        ));
    }
    
    /**
     * Release all allocated memory from all tests
     */
    @PostMapping("/release-all-memory")
    public ResponseEntity<?> releaseAllMemory() {
        int count = memoryLeakMap.size();
        memoryLeakMap.clear();
        memoryTestActive.set(false);
        
        System.gc(); // Request garbage collection
        
        return ResponseEntity.ok(Map.of(
                "releasedTests", count,
                "message", "All memory released"
        ));
    }
    
    /**
     * Helper method to release memory allocated by a specific test
     */
    private boolean releaseMemory(String testKey) {
        List<byte[]> blocks = memoryLeakMap.remove(testKey);
        if (blocks != null) {
            blocks.clear();
            if (memoryLeakMap.isEmpty()) {
                memoryTestActive.set(false);
            }
            System.gc(); // Request garbage collection
            log.info("Released memory for test: {}", testKey);
            return true;
        }
        return false;
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