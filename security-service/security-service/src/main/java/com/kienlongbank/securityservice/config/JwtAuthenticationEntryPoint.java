package com.kienlongbank.securityservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final MessageSource messageSource;
    private final LocaleResolver localeResolver;
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        log.error("Unauthorized error: {}", authException.getMessage());

        // Get locale from the request
        Locale locale = localeResolver.resolveLocale(request);
        
        // Get the appropriate error message based on the exception
        String errorMessage = getAppropriateErrorMessage(authException, locale);
        
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        final Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("error", "Unauthorized");
        body.put("message", errorMessage);
        body.put("path", request.getServletPath());

        objectMapper.writeValue(response.getOutputStream(), body);
    }
    
    private String getAppropriateErrorMessage(AuthenticationException exception, Locale locale) {
        String errorKey = "auth.error.unauthorized";
        
        // Determine the specific error based on exception type or message
        String exceptionMessage = exception.getMessage().toLowerCase();
        
        if (exceptionMessage.contains("expired")) {
            errorKey = "auth.error.expired_token";
        } else if (exceptionMessage.contains("invalid") && exceptionMessage.contains("token")) {
            errorKey = "auth.error.invalid_token";
        } else if (exceptionMessage.contains("bad credentials")) {
            errorKey = "auth.error.bad_credentials";
        }
        
        return messageSource.getMessage(errorKey, null, locale);
    }
}
