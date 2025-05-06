package com.kienlongbank.securityservice.handler;

import com.kienlongbank.securityservice.config.JwtEncryptionConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtSecurityHandler {

    @Value("${jwt.secret}")
    private String secret;
    
    private final JwtEncryptionConfig jwtEncryptionConfig;
    
    private SecretKey getSigningKey() {
        // Tạo SecretKey từ Base64-encoded string
        try {
            byte[] keyBytes = Base64.getDecoder().decode(secret);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            log.warn("Failed to decode secret as Base64, using raw bytes: {}", e.getMessage());
            return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
    }
    
    /**
     * Lấy username từ token
     * 
     * @param token JWT token cần trích xuất thông tin
     * @return Username hoặc null nếu token không hợp lệ
     */
    public String extractUsernameFromToken(String token) {
        try {
            // If token starts with "Bearer ", remove it
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // Then extract username from the decrypted JWT
            Claims claims = extractClaims(token);
            return claims.getSubject();
        } catch (Exception e) {
            log.error("Error extracting username from token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Giải mã JWT token để lấy claims
     */
    private Claims extractClaims(String token) throws Exception {
        try {
            log.debug("Parsing JWT claims");
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("Không thể giải mã JWT: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Trích xuất roles từ JWT token
     */
    public List<String> extractRoles(String token) {
        try {
            // If token starts with "Bearer ", remove it
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            Claims claims = extractClaims(token);
            Object rolesObj = claims.get("roles");
            log.debug("Roles from token: {}", rolesObj);
            if (rolesObj instanceof List<?>) {
                List<String> roles = ((List<?>) rolesObj).stream().map(String::valueOf).toList();
                log.info("Extracted roles from token: {}", roles);
                return roles;
            }
            log.warn("No roles found in token or invalid format");
            return List.of();
        } catch (Exception e) {
            log.error("Error extracting roles from token: {}", e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * Kiểm tra xem token có chứa role được yêu cầu không
     * 
     * @param token JWT token cần kiểm tra
     * @param requiredRole Role cần kiểm tra
     * @return true nếu token chứa role được yêu cầu
     */
    public boolean hasRole(String token, String requiredRole) {
        try {
            List<String> roles = extractRoles(token);
            boolean hasRole = roles.contains(requiredRole);
            log.info("Checking if token has role {}: {}", requiredRole, hasRole);
            return hasRole;
        } catch (Exception e) {
            log.error("Error checking role in token: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Kiểm tra xem token có chứa bất kỳ role nào trong danh sách được yêu cầu không
     * 
     * @param token JWT token cần kiểm tra
     * @param requiredRoles Danh sách các role cần kiểm tra
     * @return true nếu token chứa ít nhất một trong các role được yêu cầu
     */
    public boolean hasAnyRole(String token, List<String> requiredRoles) {
        try {
            if (requiredRoles == null || requiredRoles.isEmpty()) {
                log.info("No required roles specified, allowing access");
                return true; // Không yêu cầu role cụ thể
            }
            
            List<String> userRoles = extractRoles(token);
            log.debug("User roles: {}, Required roles: {}", userRoles, requiredRoles);
            
            for (String requiredRole : requiredRoles) {
                if (userRoles.contains(requiredRole)) {
                    log.info("User has required role: {}", requiredRole);
                    return true;
                }
            }
            
            log.warn("User does not have any of the required roles: {}", requiredRoles);
            return false;
        } catch (Exception e) {
            log.error("Error checking roles in token: {}", e.getMessage(), e);
            return false;
        }
    }

    public String getRoleFromToken(String jwt) {
        try {

            String decryptedJwt = jwtEncryptionConfig.decryptJweToJwt(jwt);
            // If token starts with "Bearer ", remove it
            if (decryptedJwt.startsWith("Bearer ")) {
                decryptedJwt = decryptedJwt.substring(7);
            }
            
            // First decrypt JWE to JWT

            
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(decryptedJwt)
                    .getBody();

            // Lấy roles
            Object roles = claims.get("roles");

            if (roles instanceof List<?> roleList) {
                if (!roleList.isEmpty()) {
                    return roleList.get(0).toString(); // lấy role đầu tiên
                }
            }

            return null; // hoặc throw exception nếu cần
        } catch (Exception e) {
            log.error("Error getting role from token: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Generate a properly sized key for HS256
     */
    public static SecretKey generateHS256Key() {
        return io.jsonwebtoken.security.Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256);
    }
}