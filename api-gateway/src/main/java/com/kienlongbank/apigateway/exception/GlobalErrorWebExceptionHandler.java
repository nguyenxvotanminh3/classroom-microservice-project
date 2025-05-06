package com.kienlongbank.apigateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.server.i18n.LocaleContextResolver;
import reactor.core.publisher.Mono;

import java.util.Locale;
import java.util.Map;

@Component
@Order(-2)
@Slf4j
public class GlobalErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

    private final MessageSource messageSource;
    private final LocaleContextResolver localeContextResolver;

    public GlobalErrorWebExceptionHandler(
            GatewayErrorAttributes errorAttributes,
            WebProperties.Resources resources,
            ApplicationContext applicationContext,
            ServerCodecConfigurer serverCodecConfigurer,
            MessageSource messageSource,
            LocaleContextResolver localeContextResolver) {
        super(errorAttributes, resources, applicationContext);
        this.setMessageWriters(serverCodecConfigurer.getWriters());
        this.messageSource = messageSource;
        this.localeContextResolver = localeContextResolver;
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Map<String, Object> errorPropertiesMap = getErrorAttributes(request, ErrorAttributeOptions.defaults());
        HttpStatus status = HttpStatus.valueOf((Integer) errorPropertiesMap.get("status"));
        
        // Get locale from the request
        Locale locale = localeContextResolver.resolveLocaleContext(request.exchange()).getLocale();
        if (locale == null) {
            locale = Locale.ENGLISH; // Default to English
        }
        
        // Get the error message key based on status code
        String errorKey = getErrorMessageKey(status, (String) errorPropertiesMap.get("error"));
        String localizedMessage = messageSource.getMessage(errorKey, null, (String) errorPropertiesMap.get("message"), locale);
        
        // Update the error message
        errorPropertiesMap.put("message", localizedMessage);
        
        log.debug("Rendering error response: status={}, message={}", status, localizedMessage);
        
        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(errorPropertiesMap));
    }
    
    private String getErrorMessageKey(HttpStatus status, String errorType) {
        switch (status) {
            case UNAUTHORIZED:
                return "auth.error.unauthorized";
            case FORBIDDEN:
                return "auth.error.forbidden";
            case NOT_FOUND:
                return "api.error.not_found";
            case BAD_REQUEST:
                return "api.error.bad_request";
            case METHOD_NOT_ALLOWED:
                return "api.error.method_not_allowed";
            case SERVICE_UNAVAILABLE:
                return "gateway.error.service_unavailable";
            case GATEWAY_TIMEOUT:
                return "gateway.error.timeout";
            case INTERNAL_SERVER_ERROR:
            default:
                return "api.error.server_error";
        }
    }
} 