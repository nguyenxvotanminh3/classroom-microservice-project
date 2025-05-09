package com.kienlongbank.apigateway.filter;

import com.kienlongbank.api.SecurityService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
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

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;

@Component
@Slf4j
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {


    @DubboReference(version = "1.0.0", group = "security", check = false, timeout = 5000, retries = 0)
    private SecurityService securityService;
    
    @Autowired(required = false)
    private MessageSource messageSource;

    @Value("${security.excluded.paths}")
    private String excludedPathsString;
    
    private List<String> excludedPaths;

    private final Tracer tracer;

    /**
     * Initialize filter with tracer
     */
    public AuthenticationFilter(Tracer tracer) {
        super(Config.class);
        this.tracer = tracer;
    }
    
    /**
     * Initialize excluded paths after construction
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        Span span = tracer.spanBuilder("gateway.auth.init")
            .setAttribute("component", "authentication_filter")
            .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            this.excludedPaths = Arrays.asList(excludedPathsString.split(","));
            log.info("Excluded paths for authentication: {}", excludedPaths);
            span.setAttribute("excluded_paths.count", excludedPaths.size());
            span.setAttribute("excluded_paths", excludedPathsString);
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Apply authentication filter to request
     */
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            // Kiểm tra các điều kiện bỏ qua xác thực trước khi tạo span
            if (config.isDisabled() || shouldSkipAuth(path, request.getMethod())) {
                return chain.filter(exchange);
            }

            return Mono.deferContextual(contextView -> {
                Span authSpan = tracer.spanBuilder("gateway.auth")
                    .setParent(Context.current())
                    .setAttribute("component", "authentication_filter")
                    .setAttribute("request.path", path)
                    .startSpan();
                
                try (Scope authScope = authSpan.makeCurrent()) {
                    // Kiểm tra token
                    String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                        authSpan.setAttribute("auth.error", "missing_token");
                        authSpan.setStatus(StatusCode.ERROR);
                        authSpan.end();
                        return onError(exchange, "No authorization header");
                    }

                    String token = authHeader.substring(7);
                    
                    return validateTokenAndCheckRoles(token, path, config, exchange, chain, authSpan);
                } catch (Exception e) {
                    authSpan.setAttribute("auth.error", e.getClass().getSimpleName());
                    authSpan.setStatus(StatusCode.ERROR);
                    authSpan.end();
                    return onError(exchange, "Authentication error: " + e.getMessage());
                }
            });
        };
    }

    /**
     * Check if authentication should be skipped for given path
     */
    private boolean shouldSkipAuth(String path, HttpMethod method) {
        // Kiểm tra đăng ký người dùng
        if (path.equals("/api/users") && HttpMethod.POST.equals(method)) {
            return true;
        }
        
        // Kiểm tra paths được loại trừ
        for (String excludedPath : excludedPaths) {
            if (path.trim().startsWith(excludedPath.trim())) {
                return true;
            }
        }

        // Kiểm tra Swagger
        if (isSwaggerRequest(path)) {
            return true;
        }

        // Kiểm tra actuator
        if (path.startsWith("/api/actuator/")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Validate token and check if user has required roles
     */
    private Mono<Void> validateTokenAndCheckRoles(String token, String path, Config config, 
            ServerWebExchange exchange, GatewayFilterChain chain, Span parentSpan) {
        
        Span validateSpan = tracer.spanBuilder("gateway.auth.validate_token")
            .setParent(Context.current())
            .setAttribute("token.length", token.length())
            .startSpan();
            
        return Mono.using(
            validateSpan::makeCurrent,
            validationScope -> Mono.fromCallable(() -> securityService.validateToken(token))
                .flatMap(isValid -> {
                    validateSpan.setAttribute("token.valid", isValid);
                    if (!isValid) {
                        validateSpan.setStatus(StatusCode.ERROR);
                        return Mono.error(new InvalidTokenException("Invalid token"));
                    }
                    
                    // Kiểm tra quyền nếu có danh sách quyền yêu cầu
                    if (config.getRequiredRoles() != null && !config.getRequiredRoles().isEmpty()) {
                        validateSpan.setAttribute("required_roles", String.join(",", config.getRequiredRoles()));
                        log.debug("Checking roles for path {}: {}", path, config.getRequiredRoles());
                        
                        return Mono.fromCallable(() -> securityService.hasAnyRole(token, config.getRequiredRoles()))
                            .flatMap(hasRole -> {
                                validateSpan.setAttribute("has_required_role", hasRole);
                                if (!hasRole) {
                                    validateSpan.setStatus(StatusCode.ERROR);
                                    return Mono.error(new InsufficientPrivilegesException("Insufficient privileges"));
                                }
                                return chain.filter(exchange);
                            });
                    }
                    
                    return chain.filter(exchange);
                })
                .onErrorResume(error -> {
                    String errorMessage = error.getMessage();
                    log.error("Error during token validation or role check: {}", errorMessage);
                    validateSpan.setStatus(StatusCode.ERROR);
                    validateSpan.setAttribute("error", errorMessage);
                    validateSpan.setAttribute("error.type", error.getClass().getSimpleName());
                    
                    if (error instanceof InvalidTokenException) {
                        return onError(exchange, "Invalid authentication token");
                    } else if (error instanceof InsufficientPrivilegesException) {
                        return onError(exchange, "Insufficient privileges");
                    } else {
                        return onError(exchange, "Authentication error: " + errorMessage);
                    }
                }),
            scope -> {
                validateSpan.end();
                scope.close();
            }
        );
    }
    
    /**
     * Exception thrown when token is invalid
     */
    private static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }
    }
    
    /**
     * Exception thrown when user does not have required roles
     */
    private static class InsufficientPrivilegesException extends RuntimeException {
        public InsufficientPrivilegesException(String message) {
            super(message);
        }
    }

    /**
     * Get localized message from message source
     */
    private String getLocalizedMessage(String key, String defaultMessage) {
        Span span = tracer.spanBuilder("gateway.auth.getLocalizedMessage")
            .setAttribute("message.key", key)
            .setAttribute("message.default", defaultMessage)
            .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            if (messageSource != null) {
                try {
                    String message = messageSource.getMessage(key, null, defaultMessage, LocaleContextHolder.getLocale());
                    span.setAttribute("message.resolved", message);
                    span.setAttribute("message.locale", LocaleContextHolder.getLocale().toString());
                    return message;
                } catch (Exception e) {
                    span.recordException(e);
                    span.setStatus(StatusCode.ERROR);
                    span.setAttribute("message.error", e.getMessage());
                    return defaultMessage;
                }
            }
            span.setAttribute("message.source", "not_available");
            return defaultMessage;
        } finally {
            span.end();
        }
    }

    /**
     * Check if request is for Swagger documentation
     */
    private boolean isSwaggerRequest(String path) {
        Span span = tracer.spanBuilder("gateway.auth.isSwaggerRequest")
            .setAttribute("request.path", path)
            .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            boolean isSwagger = path.contains("/v3/api-docs") || 
                   path.contains("/swagger-ui") || 
                   path.contains("/swagger-ui.html") ||
                   path.contains("/webjars/") ||
                   path.endsWith("/api-docs") ||
                   path.contains("api-docs/swagger-config");
            
            span.setAttribute("is_swagger_request", isSwagger);
            return isSwagger;
        } finally {
            span.end();
        }
    }

    /**
     * Handle authentication error with appropriate response
     */
    private Mono<Void> onError(ServerWebExchange exchange, String errorMessage) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        
        response.getHeaders().add("Content-Type", "application/json");
        
        String errorJson = String.format(
            "{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
            HttpStatus.UNAUTHORIZED.value(),
            HttpStatus.UNAUTHORIZED.getReasonPhrase(),
            errorMessage != null ? errorMessage : "No authorization header",
            exchange.getRequest().getURI().getPath()
        );
        
        return response.writeWith(
            Mono.just(response.bufferFactory().wrap(errorJson.getBytes()))
        );
    }

    /**
     * Define order of configuration shortcut fields
     */
    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("disabled", "requiredRoles");
    }

    /**
     * Create new default configuration
     */
    @Override
    public Config newConfig() {
        Config config = new Config();
        log.debug("Creating new config: {}", config);
        return config;
    }

    /**
     * Get configuration class
     */
    @Override
    public Class<Config> getConfigClass() {
        return Config.class;
    }

    /**
     * Configuration class for authentication filter
     */
    @Setter
    @Getter
    public static class Config {
        private boolean disabled;
        private List<String> requiredRoles;

        /**
         * Create default configuration with authentication enabled and no required roles
         */
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