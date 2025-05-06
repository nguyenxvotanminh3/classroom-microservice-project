package com.kienlongbank.apigateway.filter;

import com.kienlongbank.apigateway.client.SecurityServiceClient;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@Slf4j
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Autowired
    private SecurityServiceClient securityServiceClient;
    
    @Autowired(required = false)
    private MessageSource messageSource;

    @Value("${security.excluded.paths}")
    private String excludedPathsString;
    
    private List<String> excludedPaths;

    public AuthenticationFilter() {
        super(Config.class);
    }
    
    @jakarta.annotation.PostConstruct
    public void init() {
        this.excludedPaths = Arrays.asList(excludedPathsString.split(","));
        log.info("Excluded paths for authentication: {}", excludedPaths);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            log.debug("Raw config value: {}", config);
            log.info("Processing request path: {}, config disabled: {}", path, config.isDisabled());
            
            // Log request headers for debugging
            request.getHeaders().forEach((key, value) -> {
                log.debug("Request header: {} = {}", key, value);
            });

            // Skip authentication if explicitly disabled for this route (e.g., login, register)
            if (config.isDisabled()) {
                log.info("Authentication disabled for path via config: {}", path);
                return chain.filter(exchange);
            }

            // Bỏ qua xác thực cho API tạo user mới
            if (path.equals("/api/users") && HttpMethod.POST.equals(request.getMethod())) {
                log.info("Skipping authentication for user creation API: {}", path);
                return chain.filter(exchange);
            }
            
            // Skip authentication for excluded paths
            for (String excludedPath : excludedPaths) {
                if (path.trim().startsWith(excludedPath.trim())) {
                    log.info("Skipping authentication for excluded path: {}", path);
                    return chain.filter(exchange);
                }
            }

            // Skip authentication for Swagger/OpenAPI
            if (isSwaggerRequest(path)) {
                log.info("Skipping authentication for Swagger/OpenAPI path: {}", path);
                return chain.filter(exchange);
            }

            // Thêm điều kiện bỏ qua xác thực cho actuator endpoints
            if (path.startsWith("/api/actuator/")) {
                log.info("Skipping authentication for actuator endpoint: {}", path);
                return chain.filter(exchange);
            }

            log.info("Authenticating request for path: {}", path);

            // Get current locale
            Locale locale = LocaleContextHolder.getLocale();

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                log.warn("No Authorization header found for path: {}", path);
                return onError(exchange, getLocalizedMessage("auth.error.no_token", "No Authorization header"), HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            String token = authHeader;
            
            // Extract token if it contains Bearer prefix
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }

            // Validate token with security service
            log.info("validating token : " + token);
            boolean isValid = securityServiceClient.validateToken(token);
            if (!isValid) {
                log.warn("Invalid or expired token for path: {}", path);
                return onError(exchange, getLocalizedMessage("auth.error.invalid_token", "Invalid or expired token"), HttpStatus.UNAUTHORIZED);
            }

            // Check roles if required
            if (config.getRequiredRoles() != null && !config.getRequiredRoles().isEmpty()) {
                try {
                    boolean hasRequiredRole = securityServiceClient.hasAnyRole(token, config.getRequiredRoles());
                    if (!hasRequiredRole) {
                        log.warn("User does not have required roles for path: {}", path);
                        return onError(exchange, getLocalizedMessage("auth.error.forbidden", "Access denied: insufficient permissions"), HttpStatus.FORBIDDEN);
                    }
                    log.info("User has required role for path: {}", path);
                } catch (Exception e) {
                    log.error("Error checking roles: {}", e.getMessage());
                    return onError(exchange, getLocalizedMessage("auth.error.role_verification_failed", "Error verifying user permissions"), HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }

            // Get user details and add to headers
            try {
                log.info("Token is valid, proceeding with request for path: {}", path);
                
                // Add username and roles to headers for downstream services
                List<String> roles = securityServiceClient.extractRoles(token);
                log.info("roles : " + roles);
                String username = securityServiceClient.getUsernameFromToken(token);
                log.info("username : " + username);
                if (username == null) {
                    log.warn("Failed to extract username from token for path: {}", path);
                    return onError(exchange, getLocalizedMessage("auth.error.invalid_token", "Invalid token"), HttpStatus.UNAUTHORIZED);
                }
                
                // Create a new request with additional headers
                ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Roles", String.join(",", roles))
                    .header("X-Username", username)
                    .build();
                
                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            } catch (Exception e) {
                log.error("Error processing token for path: {}", path, e);
                return onError(exchange, getLocalizedMessage("api.error.server_error", "Internal server error"), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    private String getLocalizedMessage(String key, String defaultMessage) {
        if (messageSource != null) {
            try {
                return messageSource.getMessage(key, null, defaultMessage, LocaleContextHolder.getLocale());
            } catch (Exception e) {
                return defaultMessage;
            }
        }
        return defaultMessage;
    }

    private boolean isSwaggerRequest(String path) {
        return path.contains("/v3/api-docs") || 
               path.contains("/swagger-ui") || 
               path.contains("/swagger-ui.html") ||
               path.contains("/webjars/") ||
               path.endsWith("/api-docs") ||
               path.contains("api-docs/swagger-config");
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        
        // Set header for json response
        response.getHeaders().add("Content-Type", "application/json");
        
        // Create error response body
        String errorJson = String.format(
            "{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
            status.value(),
            status.getReasonPhrase(),
            message,
            exchange.getRequest().getURI().getPath()
        );
        
        // Write error response to body
        return response.writeWith(
            Mono.just(response.bufferFactory().wrap(errorJson.getBytes()))
        );
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("disabled", "requiredRoles");
    }

    @Override
    public Config newConfig() {
        Config config = new Config();
        log.debug("Creating new config: {}", config);
        return config;
    }

    @Override
    public Class<Config> getConfigClass() {
        return Config.class;
    }

    @Setter
    @Getter
    public static class Config {
        private boolean disabled;
        private List<String> requiredRoles;

        public Config() {
            this.disabled = false;
            this.requiredRoles = new ArrayList<>();
        }
        
        @Override
        public String toString() {
            return "Config{disabled=" + disabled + ", requiredRoles=" + requiredRoles + "}";
        }
    }
} 