package com.kienlongbank.nguyenminh.user.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/circuit-breaker-monitor")
@RequiredArgsConstructor
@Slf4j
public class CircuitBreakerMonitorController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * Get all circuit breaker names in the application
     */
    @GetMapping("/names")
    public ResponseEntity<?> getAllCircuitBreakerNames() {
        Set<String> names = circuitBreakerRegistry.getAllCircuitBreakers()
                .stream()
                .map(CircuitBreaker::getName)
                .collect(Collectors.toSet());
        
        return ResponseEntity.ok(Map.of("circuitBreakers", names));
    }

    /**
     * Get the state and metrics of all circuit breakers
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllCircuitBreakers() {
        List<Map<String, Object>> breakers = new ArrayList<>();
        
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(circuitBreaker -> {
            breakers.add(getCircuitBreakerDetails(circuitBreaker));
        });
        
        return ResponseEntity.ok(Map.of("circuitBreakers", breakers));
    }

    /**
     * Get the state and metrics of a specific circuit breaker
     */
    @GetMapping("/{name}")
    public ResponseEntity<?> getCircuitBreakerByName(@PathVariable String name) {
        try {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);
            return ResponseEntity.ok(getCircuitBreakerDetails(circuitBreaker));
        } catch (Exception e) {
            log.error("Error getting circuit breaker {}: {}", name, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Circuit breaker not found",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Force a circuit breaker into a specific state (useful for testing)
     */
    @GetMapping("/{name}/force/{state}")
    public ResponseEntity<?> forceCircuitBreakerState(
            @PathVariable String name,
            @PathVariable String state) {
        
        try {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);
            
            switch (state.toLowerCase()) {
                case "open":
                    circuitBreaker.transitionToOpenState();
                    break;
                case "half-open":
                    circuitBreaker.transitionToHalfOpenState();
                    break;
                case "closed":
                    circuitBreaker.transitionToClosedState();
                    break;
                case "disabled":
                    circuitBreaker.transitionToDisabledState();
                    break;
                case "metrics-only":
                    circuitBreaker.transitionToMetricsOnlyState();
                    break;
                case "forced-open":
                    circuitBreaker.transitionToForcedOpenState();
                    break;
                default:
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "Invalid state",
                            "message", "Valid states are: open, half-open, closed, disabled, metrics-only, forced-open"
                    ));
            }
            
            return ResponseEntity.ok(Map.of(
                    "name", name,
                    "newState", circuitBreaker.getState(),
                    "message", "Circuit breaker state changed successfully"
            ));
            
        } catch (Exception e) {
            log.error("Error changing circuit breaker state {}: {}", name, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Error changing circuit breaker state",
                    "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Reset a circuit breaker's metrics
     */
    @GetMapping("/{name}/reset")
    public ResponseEntity<?> resetCircuitBreakerMetrics(@PathVariable String name) {
        try {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);
            circuitBreaker.reset();
            
            return ResponseEntity.ok(Map.of(
                    "name", name,
                    "state", circuitBreaker.getState(),
                    "message", "Circuit breaker metrics reset successfully"
            ));
            
        } catch (Exception e) {
            log.error("Error resetting circuit breaker {}: {}", name, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Error resetting circuit breaker",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Helper method to get circuit breaker details
     */
    private Map<String, Object> getCircuitBreakerDetails(CircuitBreaker circuitBreaker) {
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        Map<String, Object> config = new HashMap<>();
        CircuitBreaker.State state = circuitBreaker.getState();
        
        // Add metrics
        Map<String, Object> metricsMap = new HashMap<>();
        metricsMap.put("failureRate", metrics.getFailureRate());
        metricsMap.put("slowCallRate", metrics.getSlowCallRate());
        metricsMap.put("numberOfSuccessfulCalls", metrics.getNumberOfSuccessfulCalls());
        metricsMap.put("numberOfFailedCalls", metrics.getNumberOfFailedCalls());
        metricsMap.put("numberOfSlowCalls", metrics.getNumberOfSlowCalls());
        metricsMap.put("numberOfNotPermittedCalls", metrics.getNumberOfNotPermittedCalls());
        
        // Add configuration - simplified to avoid linter errors with specific getters
        try {
            config.put("failureRateThreshold", circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold());
            config.put("slowCallRateThreshold", circuitBreaker.getCircuitBreakerConfig().getSlowCallRateThreshold());
            config.put("permittedNumberOfCallsInHalfOpenState", 
                    circuitBreaker.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState());
            config.put("slidingWindowSize", circuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize());
            config.put("minimumNumberOfCalls", circuitBreaker.getCircuitBreakerConfig().getMinimumNumberOfCalls());
            
            // Config as string to handle possible incompatible API versions
            config.put("configDetails", circuitBreaker.getCircuitBreakerConfig().toString());
        } catch (Exception e) {
            log.warn("Error getting some circuit breaker configuration details: {}", e.getMessage());
            config.put("error", "Some configuration properties could not be accessed");
        }
        
        Map<String, Object> details = new HashMap<>();
        details.put("name", circuitBreaker.getName());
        details.put("state", state);
        details.put("metrics", metricsMap);
        details.put("config", config);
        
        return details;
    }
} 