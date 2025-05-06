package com.kienlongbank.apigateway.config;

import com.kienlongbank.apigateway.filter.AuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class GatewayRoutesConfig {

    @Autowired
    private AuthenticationFilter authenticationFilter;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()

            // User Service API Docs
            .route("user-service-api-docs", r -> r
                .path("/user-api/v3/api-docs")
                .filters(f -> f.rewritePath("/user-api/v3/api-docs", "/api/v3/api-docs"))
                .uri("http://localhost:8080"))
                
            // Security Service API Docs
            .route("security-service-api-docs", r -> r
                .path("/security-api/v3/api-docs")
                .filters(f -> f.rewritePath("/security-api/v3/api-docs", "/api/v3/api-docs"))
                .uri("http://localhost:8081"))
                
            // Classroom Service API Docs
            .route("classroom-service-api-docs", r -> r
                .path("/classroom-api/v3/api-docs")
                .filters(f -> f.rewritePath("/classroom-api/v3/api-docs", "/api/v3/api-docs"))
                .uri("http://localhost:8082"))
                
            // Email Service API Docs
            .route("email-service-api-docs", r -> r
                .path("/email-api/v3/api-docs")
                .filters(f -> f.rewritePath("/email-api/v3/api-docs", "/api/v3/api-docs"))
                .uri("http://localhost:8083"))
                
            // Security Service General API Route
            .route("security-service-api", r -> r
                .path("/security-api/**")
                .filters(f -> f
                    .rewritePath("/security-api/(?<segment>.*)", "/api/${segment}")
                    .filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
                .uri("http://localhost:8081"))
                
            // User Service General API Route
            // NOTE: This route might conflict with more specific routes defined elsewhere (e.g., in application.yml or RouteConfig.java)
            // Ensure proper 'order' is set if using multiple configuration sources.
            .route("user-service-api", r -> r
                .path("/user-api/**")
                .filters(f -> {
                    // Create a specific config for this route requiring ADMIN role
                    AuthenticationFilter.Config config = new AuthenticationFilter.Config();
                    config.setRequiredRoles(List.of("ADMIN")); // Require ADMIN role
                    return f
                        .rewritePath("/user-api/(?<segment>.*)", "/api/${segment}")
                        .filter(authenticationFilter.apply(config)); // Apply filter with the ADMIN requirement
                })
                .uri("http://localhost:8080")) // Consider using lb://user-service if service discovery is intended
                
            // Classroom Service General API Route
            .route("classroom-service-api", r -> r
                .path("/classroom-api/**")
                .filters(f -> f
                    .rewritePath("/classroom-api/(?<segment>.*)", "/api/${segment}")
                    .filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
                .uri("http://localhost:8082"))
                
            // Email Service General API Route
            .route("email-service-api", r -> r
                .path("/email-api/**")
                .filters(f -> f
                    .rewritePath("/email-api/(?<segment>.*)", "/api/${segment}")
                    .filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
                .uri("http://localhost:8083"))
                
            .build();
    }
} 