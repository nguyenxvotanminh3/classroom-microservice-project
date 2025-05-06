package com.kienlongbank.securityservice.controller;
import com.kienlongbank.securityservice.dto.LoginRequest;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.kienlongbank.securityservice.handler.AuthHandler;

@RestController
@RequestMapping({"/api/auth", "/auth"})
@CrossOrigin("*")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final Tracer tracer;
    private final AuthHandler authHandler;



    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        return authHandler.handleAuthenticateUser(loginRequest, request);
    }
    
    private ResponseEntity<?> authenticateUserDirect(LoginRequest loginRequest) {
        return authHandler.handleAuthenticateUserDirect(loginRequest);
    }
    
    @PostMapping("/login/success")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> loginSuccess() {
        return authHandler.handleLoginSuccess();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        return authHandler.handleLogout(request, response);
    }

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return authHandler.handleHealthCheck();
    }
}
