package com.kienlongbank.securityservice.handler;

import com.kienlongbank.securityservice.dto.LoginRequest;
import com.kienlongbank.securityservice.service.AuthenticateService;
import com.kienlongbank.securityservice.config.JwtUtils;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthHandler {
    private final AuthenticateService authenticateService;
    private final MessageSource messageSource;
    private final Tracer tracer;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    public ResponseEntity<?> handleAuthenticateUser(LoginRequest loginRequest, HttpServletRequest request) {
        Span span = tracer.nextSpan().name("authenticateUser").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            log.info("Authentication attempt for user: {}", loginRequest.getUserName());
            span.tag("user.name", loginRequest.getUserName());

            Locale locale = request.getLocale();
            return authenticateService.authenticateUser(loginRequest, locale);
        } catch (Exception e) {
            String msg = messageSource.getMessage("login.failed", null, request.getLocale());
            span.tag("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", msg));
        } finally {
            span.end();
        }
    }

    public ResponseEntity<?> handleAuthenticateUserDirect(LoginRequest loginRequest) {
        Span span = tracer.nextSpan().name("authenticateUserDirect").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            log.info("Falling back to direct authentication for: {}", loginRequest.getUserName());
            span.tag("user.name", loginRequest.getUserName());
            span.tag("auth.method", "direct");

            // Check if this is the emergency user with exact match for credentials
            if (loginRequest.getUserName().equals("nguyenxvotanminh") &&
                loginRequest.getPassword().equals("1234567890")) {

                log.info("Emergency direct authentication successful for: {}", loginRequest.getUserName());
                span.tag("auth.type", "emergency");

                // Create a simple UserDetails
                UserDetails userDetails = User.builder()
                    .username(loginRequest.getUserName())
                    .password(passwordEncoder.encode(loginRequest.getPassword()))
                    .authorities("ADMIN", "USER")
                    .build();

                // Generate JWT
                String jwt = jwtUtils.generateJwtToken(userDetails);

                // Create response
                Map<String, Object> response = new HashMap<>();
                response.put("token", jwt);
                response.put("username", loginRequest.getUserName());
                response.put("roles", "ADMIN,USER");
                response.put("authMethod", "direct-emergency");

                return ResponseEntity.ok(response);
            }

            // For non-emergency users, try the normal authentication flow
            try {
                Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUserName(), loginRequest.getPassword())
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Get user details
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();

                // Generate JWT
                String jwt = jwtUtils.generateJwtToken(userDetails);
                String username = userDetails.getUsername();
                String roles = userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(","));

                // Create response
                Map<String, Object> response = new HashMap<>();
                response.put("token", jwt);
                response.put("username", username);
                response.put("roles", roles);
                response.put("authMethod", "direct");

                log.info("Direct authentication successful for: {}", loginRequest.getUserName());
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                log.error("Direct authentication failed: {}", e.getMessage(), e);
                span.tag("error", e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            span.end();
        }
    }

    public ResponseEntity<?> handleLogout(HttpServletRequest request, HttpServletResponse response) {
        Span span = tracer.nextSpan().name("logout").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            log.info("Logout attempt");
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwtToken = authHeader.substring(7);
                return ResponseEntity.ok().build();
            } else {
                span.tag("error", "Invalid or missing JWT token");
                return ResponseEntity.badRequest().body("Invalid or missing JWT token");
            }
        } finally {
            span.end();
        }
    }

    public ResponseEntity<?> handleLoginSuccess() {
        Span span = tracer.nextSpan().name("loginSuccess").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            log.info("Login success endpoint called");
            return ResponseEntity.ok("Login successful!");
        } finally {
            span.end();
        }
    }

    public ResponseEntity<?> handleHealthCheck() {
        Span span = tracer.nextSpan().name("healthCheck").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            log.info("Health check called");
            return ResponseEntity.ok("Auth service is running");
        } finally {
            span.end();
        }
    }
} 