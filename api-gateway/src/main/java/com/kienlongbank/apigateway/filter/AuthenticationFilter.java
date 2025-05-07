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

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;

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

    private final Tracer tracer;

    public AuthenticationFilter(Tracer tracer) {
        super(Config.class);
        this.tracer = tracer;
    }
    
    @jakarta.annotation.PostConstruct
    public void init() {
        this.excludedPaths = Arrays.asList(excludedPathsString.split(","));
        log.info("Excluded paths for authentication: {}", excludedPaths);
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
                            return onError(exchange);
                        }

                        String token = authHeader.substring(7);
                        
                        Span validateSpan = tracer.spanBuilder("gateway.auth.validate_token")
                            .setAttribute("token.length", token.length())
                            .startSpan();

                        return Mono.using(
                            validateSpan::makeCurrent,
                            validationScope -> Mono.fromCallable(() -> securityServiceClient.validateToken(token))
                                .map(isValid -> {
                                    validateSpan.setAttribute("token.valid", isValid);
                                    if (!isValid) {
                                        validateSpan.setStatus(StatusCode.ERROR);
                                        throw new RuntimeException("Invalid token");
                                    }
                                    return true;
                                })
                                .then(chain.filter(exchange))
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

    private Mono<Void> onError(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        
        response.getHeaders().add("Content-Type", "application/json");
        
        String errorJson = String.format(
            "{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
            HttpStatus.UNAUTHORIZED.value(),
            HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "No authorization header",
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