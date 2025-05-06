package com.kienlongbank.apigateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Component
@Slf4j
public class GatewayErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Map<String, Object> errorAttributes = super.getErrorAttributes(request, options);
        
        Throwable error = getError(request);
        log.error("Gateway Error: {}", error.getMessage(), error);
        
        // Add any custom error information here if needed
        if (error instanceof ResponseStatusException) {
            ResponseStatusException ex = (ResponseStatusException) error;
            errorAttributes.put("message", ex.getReason());
        }
        
        return errorAttributes;
    }
} 