package com.kienlongbank.apigateway.config;

import com.kienlongbank.apigateway.filter.AuthenticationFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import java.util.Arrays;
import java.util.function.Consumer;

@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder, AuthenticationFilter authenticationFilter) {
        return builder.routes()
            // User Service Routes
            .route("user-get-all", r -> r.path("/user-api/users")
                .and().method(HttpMethod.GET)
                .filters(f -> f
                    .filter(authenticationFilter.apply(createRoleConfig("ADMIN")))
                    .rewritePath("/user-api/(?<segment>.*)", "/api/${segment}"))
                .uri("lb://user-service"))
            .route("user-get-by-id", r -> r.path("/user-api/users/{id}")
                .and().method(HttpMethod.GET)
                .filters(f -> f
                    .filter(authenticationFilter.apply(createRoleConfig("ADMIN")))
                    .rewritePath("/user-api/(?<segment>.*)", "/api/${segment}"))
                .uri("lb://user-service"))
            .route("user-other", r -> r.path("/user-api/**")
                .filters(f -> f
                    .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                    .rewritePath("/user-api/(?<segment>.*)", "/api/${segment}"))
                .uri("lb://user-service"))
            // Security Service Routes
            .route("security-service", r -> r.path("/auth/**")
                .filters(f -> f
                    .filter(authenticationFilter.apply(createDisabledConfig()))
                    .rewritePath("/auth/(?<segment>.*)", "/api/${segment}"))
                .uri("lb://security-service"))
            .build();
    }
    
    private Consumer<AuthenticationFilter.Config> createRoleConfig(String... roles) {
        return config -> {
            config.setRequiredRoles(Arrays.asList(roles));
        };
    }
    
    private Consumer<AuthenticationFilter.Config> createDisabledConfig() {
        return config -> {
            config.setDisabled(true);
        };
    }
} 