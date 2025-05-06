package com.kienlongbank.nguyenminh.user.controller;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/resilience-test")
@Slf4j
public class ResilienceController {

    private static final String RESILIENCE_TEST = "resilienceTest";
    private int retryCount = 0;

    @GetMapping("/circuit-breaker")
    @CircuitBreaker(name = RESILIENCE_TEST, fallbackMethod = "circuitBreakerFallback")
    public ResponseEntity<Map<String, Object>> testCircuitBreaker() {
        // Simulate failure 50% of the time
        if (Math.random() > 0.5) {
            throw new RuntimeException("Random failure to test circuit breaker!");
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Circuit Breaker test successful");
        response.put("status", "SUCCESS");
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<Map<String, Object>> circuitBreakerFallback(Throwable t) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Circuit breaker is open! Fallback executed.");
        response.put("error", t.getMessage());
        response.put("status", "FALLBACK");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/retry")
    @Retry(name = RESILIENCE_TEST, fallbackMethod = "retryFallback")
    public ResponseEntity<Map<String, Object>> testRetry() {
        log.info("Retry attempt: {}", ++retryCount);
        // Fail first 2 attempts
        if (retryCount <= 2) {
            throw new RuntimeException("Simulated failure for retry test!");
        }
        
        // Reset counter for next test
        retryCount = 0;
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Retry test successful after attempts");
        response.put("status", "SUCCESS");
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<Map<String, Object>> retryFallback(Throwable t) {
        retryCount = 0; // Reset for next test
        Map<String, Object> response = new HashMap<>();
        response.put("message", "All retry attempts failed!");
        response.put("error", t.getMessage());
        response.put("status", "FALLBACK");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rate-limiter")
    @RateLimiter(name = RESILIENCE_TEST)
    public ResponseEntity<Map<String, Object>> testRateLimiter() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Rate limiter allowed the request");
        response.put("status", "SUCCESS");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/bulkhead")
    @Bulkhead(name = RESILIENCE_TEST, fallbackMethod = "bulkheadFallback")
    public ResponseEntity<Map<String, Object>> testBulkhead() {
        // Simulate long processing
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Bulkhead test successful");
        response.put("status", "SUCCESS");
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<Map<String, Object>> bulkheadFallback(Throwable t) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Bulkhead limit reached! Request rejected.");
        response.put("error", t.getMessage());
        response.put("status", "FALLBACK");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/timeout")
    @TimeLimiter(name = RESILIENCE_TEST, fallbackMethod = "timeoutFallback")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> testTimeout() {
        return CompletableFuture.supplyAsync(() -> {
            // Simulate slow processing
            try {
                // Random delay between 0-6 seconds
                long delay = (long) (Math.random() * 6000);
                log.info("Timeout test will delay for {} ms", delay);
                TimeUnit.MILLISECONDS.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Timeout test completed successfully");
            response.put("status", "SUCCESS");
            return ResponseEntity.ok(response);
        });
    }

    public CompletableFuture<ResponseEntity<Map<String, Object>>> timeoutFallback(Throwable t) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Request timed out!");
        response.put("error", t.getMessage());
        response.put("status", "FALLBACK");
        return CompletableFuture.completedFuture(ResponseEntity.ok(response));
    }

    @GetMapping("/combined")
    @CircuitBreaker(name = RESILIENCE_TEST, fallbackMethod = "combinedFallback")
    @RateLimiter(name = RESILIENCE_TEST)
    @Bulkhead(name = RESILIENCE_TEST)
    @Retry(name = RESILIENCE_TEST)
    public ResponseEntity<Map<String, Object>> testCombined() {
        // Random failure 30% of the time
        if (Math.random() < 0.3) {
            throw new RuntimeException("Random failure in combined test!");
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Combined resilience patterns test successful");
        response.put("status", "SUCCESS");
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<Map<String, Object>> combinedFallback(Throwable t) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Combined test fallback executed");
        response.put("error", t.getMessage());
        response.put("status", "FALLBACK");
        return ResponseEntity.ok(response);
    }
} 