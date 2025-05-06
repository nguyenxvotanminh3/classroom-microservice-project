package com.kienlongbank.securityservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kienlongbank.securityservice.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.LocaleResolver;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final JwtEncryptionConfig jwtEncryptionConfig;
    private final MessageSource messageSource;
    private final LocaleResolver localeResolver;
    private final ObjectMapper objectMapper;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("doFilterInternal");
        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        // Resolve locale for i18n
        Locale locale = localeResolver.resolveLocale(request);

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            log.info("Processing token");
            try {
                // Cut "Bearer " to get JWE
                String encryptedToken = authorizationHeader.substring(7);

                // Decrypt JWE to get JWT
                jwt = jwtEncryptionConfig.decryptJweToJwt(encryptedToken);
                log.info("Decrypted JWT: {}", jwt);
                
                // Extract username from JWT
                username = jwtUtils.extractUsername(jwt);
                log.info("Extracted username: {}", username);

            } catch (Exception e) {
                log.error("Failed to process token: {}", e.getMessage());
                handleTokenError(request, response, e, locale);
                return;
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                if (jwtUtils.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    log.info("Authentication successful for user: " + username);
                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                } else {
                    log.warn("JWT validation failed for user: " + username);
                    handleInvalidTokenError(request, response, locale);
                    return;
                }
            } catch (Exception e) {
                log.error("Error authenticating user: " + e.getMessage());
                handleTokenError(request, response, e, locale);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
    
    private void handleTokenError(HttpServletRequest request, HttpServletResponse response, Exception e, Locale locale) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        String errorKey = "auth.error.unauthorized";
        String errorMessage = e.getMessage().toLowerCase();
        
        if (errorMessage.contains("expired")) {
            errorKey = "auth.error.expired_token";
        } else if (errorMessage.contains("invalid") || errorMessage.contains("malformed")) {
            errorKey = "auth.error.invalid_token";
        }
        
        String localizedMessage = messageSource.getMessage(errorKey, null, locale);
        
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("error", "Unauthorized");
        body.put("message", localizedMessage);
        body.put("path", request.getServletPath());
        
        objectMapper.writeValue(response.getOutputStream(), body);
    }
    
    private void handleInvalidTokenError(HttpServletRequest request, HttpServletResponse response, Locale locale) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        String localizedMessage = messageSource.getMessage("auth.error.invalid_token", null, locale);
        
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("error", "Unauthorized");
        body.put("message", localizedMessage);
        body.put("path", request.getServletPath());
        
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}