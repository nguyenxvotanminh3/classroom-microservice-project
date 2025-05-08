package com.kienlongbank.apigateway.filter;

import com.kienlongbank.api.SecurityService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
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

    public AuthenticationFilter(Tracer tracer) {
        super(Config.class);
        this.tracer = tracer;
    }
    
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

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            return Mono.deferContextual(contextView -> {
                Span authSpan = tracer.spanBuilder("gateway.auth")
                    .setParent(Context.current())
                    .setAttribute("component", "authentication_filter")
                    .startSpan();

                return Mono.using(
                    authSpan::makeCurrent,
                    authScope -> {
                        ServerHttpRequest request = exchange.getRequest();
                        String path = request.getURI().getPath();
                        
                        authSpan.setAttribute("request.path", path);
                        
                        if (config.isDisabled()) {
                            authSpan.setAttribute("auth.skipped", true);
                            return chain.filter(exchange);
                        }

                        if (path.equals("/api/users") && HttpMethod.POST.equals(request.getMethod())) {
                            authSpan.setAttribute("auth.skipped", "user_creation");
                            return chain.filter(exchange);
                        }
                        
                        for (String excludedPath : excludedPaths) {
                            if (path.trim().startsWith(excludedPath.trim())) {
                                authSpan.setAttribute("auth.skipped", "excluded_path");
                                return chain.filter(exchange);
                            }
                        }

                        if (isSwaggerRequest(path)) {
                            authSpan.setAttribute("auth.skipped", "swagger");
                            return chain.filter(exchange);
                        }

                        if (path.startsWith("/api/actuator/")) {
                            authSpan.setAttribute("auth.skipped", "actuator");
                            return chain.filter(exchange);
                        }

                        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                            authSpan.setAttribute("auth.error", "missing_token");
                            authSpan.setStatus(StatusCode.ERROR);
                            return onError(exchange, "No authorization header");
                        }

                        String token = authHeader.substring(7);
                        
                        Span validateSpan = tracer.spanBuilder("gateway.auth.validate_token")
                            .setAttribute("token.length", token.length())
                            .startSpan();

                        return Mono.using(
                            validateSpan::makeCurrent,
                            validationScope -> Mono.fromCallable(() -> securityService.validateToken(token))
                                .flatMap(isValid -> {
                                    validateSpan.setAttribute("token.valid", isValid);
                                    if (!isValid) {
                                        validateSpan.setStatus(StatusCode.ERROR);
                                        return Mono.error(new RuntimeException("Invalid token"));
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
                                                    return onError(exchange, "Insufficient privileges");
                                                }
                                                return chain.filter(exchange);
                                            });
                                    }
                                    
                                    return chain.filter(exchange);
                                })
                                .onErrorResume(error -> {
                                    log.error("Error during token validation or role check: {}", error.getMessage());
                                    validateSpan.setStatus(StatusCode.ERROR);
                                    validateSpan.setAttribute("error", error.getMessage());
                                    return onError(exchange, error.getMessage());
                                })
                                .doFinally(signalType -> validateSpan.end()),
                                Scope::close
                        );
                    },
                    authScope -> {
                        authSpan.end();
                        authScope.close();
                    }
                );
            });
        };
    }

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