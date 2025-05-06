package com.kienlongbank.securityservice.service;

import com.kienlongbank.api.security.JwtValidationService;
import com.kienlongbank.securityservice.config.JwtEncryptionConfig;
import com.kienlongbank.securityservice.config.JwtUtils;
import com.kienlongbank.securityservice.handler.JwtSecurityHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * Implementation Dubbo service để xác thực JWT token
 */
@Service
@DubboService(version = "1.0.0", group = "security-jwt-related", interfaceClass = com.kienlongbank.api.security.JwtValidationService.class)
@Slf4j
@RequiredArgsConstructor
public class JwtValidationServiceImpl implements JwtValidationService {

    private final JwtUtils jwtUtils;
    private final JwtEncryptionConfig jwtEncryptionConfig;
    private final JwtSecurityHandler jwtSecurityHandler;

    @Override
    public boolean validateTokenForUsername(String token, String requestedUsername) {
        try {
            log.info("Validating token for username: {}", requestedUsername);
            // Giải mã JWE token để lấy JWT
            String jwt = jwtEncryptionConfig.decryptJweToJwt(token);
            
            // Lấy username từ token
            String usernameFromToken = jwtUtils.extractUsername(jwt);
            
            // Kiểm tra xem username từ token có trùng với username được yêu cầu không
            log.info("Username from token: {}, Requested username: {}", usernameFromToken, requestedUsername);
            
            // Kiểm tra token còn hạn không
            Date expirationDate = jwtUtils.extractExpiration(jwt);
            boolean isExpired = expirationDate == null || expirationDate.before(new Date());
            
            return usernameFromToken != null && 
                   usernameFromToken.equals(requestedUsername) && 
                   !isExpired;
        } catch (Exception e) {
            log.error("Error validating token for username: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getUsernameFromToken(String token) {
        try {
            // Giải mã JWE token để lấy JWT
            String jwt = jwtEncryptionConfig.decryptJweToJwt(token);
            
            // Lấy username từ token
            return jwtUtils.extractUsername(jwt);
        } catch (Exception e) {
            log.error("Error extracting username from token: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String getRoleFromToken(String token) {
        try {

            
            // Lấy role từ token
            return jwtSecurityHandler.getRoleFromToken(token);
        } catch (Exception e) {
            log.error("Error extracting role from token: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean validateToken(String token) {
        try {
            // Giải mã JWE token để lấy JWT
            String jwt = jwtEncryptionConfig.decryptJweToJwt(token);
            
            // Kiểm tra xem token có hợp lệ không (chưa hết hạn)
            Date expirationDate = jwtUtils.extractExpiration(jwt);
            return expirationDate != null && expirationDate.after(new Date());
        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<String> extractRoles(String token) {
        try {
            // Giải mã JWE token để lấy JWT
            String jwt = jwtEncryptionConfig.decryptJweToJwt(token);
            
            // Lấy roles từ token
            return jwtSecurityHandler.extractRoles(jwt);
        } catch (Exception e) {
            log.error("Error extracting roles from token: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public boolean hasRole(String token, String role) {
        try {
            // Giải mã JWE token để lấy JWT
            String jwt = jwtEncryptionConfig.decryptJweToJwt(token);
            
            // Kiểm tra role
            return jwtSecurityHandler.hasRole(jwt, role);
        } catch (Exception e) {
            log.error("Error checking role in token: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean hasAnyRole(String token, List<String> roles) {
        try {
            // Giải mã JWE token để lấy JWT
            String jwt = jwtEncryptionConfig.decryptJweToJwt(token);
            
            // Kiểm tra role
            return jwtSecurityHandler.hasAnyRole(jwt, roles);
        } catch (Exception e) {
            log.error("Error checking roles in token: {}", e.getMessage());
            return false;
        }
    }
} 