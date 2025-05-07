package com.kienlongbank.apigateway.config;

import com.kienlongbank.apigateway.filter.AuthenticationFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Cấu hình tổng hợp cho API Gateway routes
 * Gộp cả RouteConfig và GatewayRoutesConfig
 */
@Configuration
public class GatewayRouteConfig {

    @Autowired
    private AuthenticationFilter authenticationFilter;

    @Bean
    public RouteLocator combinedServiceRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
            // ===== API DOCS ROUTES =====
            
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
                
            // ===== USER SERVICE ROUTES =====
            
            // Specific User Routes with Method check
            .route("user-get-all", r -> r.path("/user-api/users")
                .and().method(HttpMethod.GET)
                .filters(f -> f
                    .filter(authenticationFilter.apply(createRoleConfig("ADMIN")))
                    .rewritePath("/user-api/(?<segment>.*)", "/api/${segment}"))
                .uri("http://localhost:8080"))
                
            .route("user-get-by-id", r -> r.path("/user-api/users/{id}")
                .and().method(HttpMethod.GET)
                .filters(f -> f
                    .filter(authenticationFilter.apply(createRoleConfig("ADMIN")))
                    .rewritePath("/user-api/(?<segment>.*)", "/api/${segment}"))
                .uri("http://localhost:8080"))
                
            // User management routes (create, update, delete) - Admin only
            .route("user-create", r -> r.path("/user-api/users")
                .and().method(HttpMethod.POST)
                .filters(f -> f
                    .filter(authenticationFilter.apply(createRoleConfig("ADMIN")))
                    .rewritePath("/user-api/(?<segment>.*)", "/api/${segment}"))
                .uri("http://localhost:8080"))
                
            .route("user-update", r -> r.path("/user-api/users/{id}")
                .and().method(HttpMethod.PUT)
                .filters(f -> f
                    .filter(authenticationFilter.apply(createRoleConfig("ADMIN")))
                    .rewritePath("/user-api/(?<segment>.*)", "/api/${segment}"))
                .uri("http://localhost:8080"))
                
            .route("user-delete", r -> r.path("/user-api/users/{id}")
                .and().method(HttpMethod.DELETE)
                .filters(f -> f
                    .filter(authenticationFilter.apply(createRoleConfig("ADMIN")))
                    .rewritePath("/user-api/(?<segment>.*)", "/api/${segment}"))
                .uri("http://localhost:8080"))
                
            // General User Service Route (catch-all)
            .route("user-other", r -> r.path("/user-api/**")
                .filters(f -> f
                    .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                    .rewritePath("/user-api/(?<segment>.*)", "/api/${segment}"))
                .uri("http://localhost:8080"))
                
            // ===== SECURITY SERVICE ROUTES =====
            
            // Old compatibility route for /auth/**
            .route("security-auth", r -> r.path("/auth/**")
                .filters(f -> f
                    .filter(authenticationFilter.apply(createDisabledConfig()))
                    .rewritePath("/auth/(?<segment>.*)", "/api/${segment}"))
                .uri("http://localhost:8081"))
                
            // Security Authentication Endpoints (Login/Register) - NO AUTHENTICATION
            .route("security-auth-endpoints", r -> r
                .path("/security-api/auth/login", "/security-api/auth/register")
                .filters(f -> f
                    .rewritePath("/security-api/(?<segment>.*)", "/api/${segment}")
                    .filter(authenticationFilter.apply(createDisabledConfig())))
                .uri("http://localhost:8081"))
                
            // Regular Security Service endpoints
            .route("security-service-api", r -> r
                .path("/security-api/**")
                .filters(f -> f
                    .rewritePath("/security-api/(?<segment>.*)", "/api/${segment}")
                    .filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
                .uri("http://localhost:8081"))
                
            // ===== CLASSROOM SERVICE ROUTES =====
            
            .route("classroom-service-api", r -> r
                .path("/classroom-api/**")
                .filters(f -> f
                    .rewritePath("/classroom-api/(?<segment>.*)", "/api/${segment}")
                    .filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
                .uri("http://localhost:8082"))
                
            // ===== EMAIL SERVICE ROUTES =====
            
            .route("email-service-api", r -> r
                .path("/email-api/**")
                .filters(f -> f
                    .rewritePath("/email-api/(?<segment>.*)", "/api/${segment}")
                    .filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
                .uri("http://localhost:8083"))
                
            .build();
    }
    
    /**
     * Helper method để tạo cấu hình với các role yêu cầu
     */
    private Consumer<AuthenticationFilter.Config> createRoleConfig(String... roles) {
        return config -> {
            config.setRequiredRoles(Arrays.asList(roles));
        };
    }
    
    /**
     * Helper method để tạo cấu hình vô hiệu hóa xác thực
     */
    private Consumer<AuthenticationFilter.Config> createDisabledConfig() {
        return config -> {
            config.setDisabled(true);
        };
    }
} 