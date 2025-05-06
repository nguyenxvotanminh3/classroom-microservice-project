package com.kienlongbank.nguyenminh.user.handler;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Component
@Slf4j
public class JwtSecurityHandler {

    @Value("${jwt.secret}")
    private String secret;

    /**
     * Lấy token từ HttpServletRequest
     * 
     * @param request HttpServletRequest chứa Authorization header
     * @return JWT token đã được trích xuất, hoặc null nếu không có token hợp lệ
     */
    public String extractTokenFromRequest(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        
        return authorizationHeader.substring(7);
    }
    
    /**
     * Lấy username từ token
     * 
     * @param token JWT token cần trích xuất thông tin
     * @return Username hoặc null nếu token không hợp lệ
     */
    public String extractUsernameFromToken(String token) {
        try {
            return extractTokenUsername(token);
        } catch (Exception e) {
            log.error("Error extracting username from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Kiểm tra xem người dùng hiện tại có quyền truy cập tài nguyên của username không
     * 
     * @param request HttpServletRequest chứa JWT token
     * @param requestedUsername Username của người dùng mà API đang truy cập thông tin
     * @throws ResponseStatusException nếu không có quyền truy cập
     */
    public void validateUserAccess(HttpServletRequest request, String requestedUsername) {
        String authorizationHeader = request.getHeader("Authorization");
        
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.error("Authorization header không hợp lệ");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không có quyền truy cập");
        }
        
        // Lấy token từ header
        String token = authorizationHeader.substring(7);
        log.info("Token: {}", token);
        
        try {
            // Lấy username từ JWT token
            String tokenUsername = extractTokenUsername(token);
            if (tokenUsername == null) {
                log.error("Không lấy được username từ token");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token không hợp lệ hoặc đã hết hạn");
            }
            log.info("Username từ token: {}, username được yêu cầu: {}", tokenUsername, requestedUsername);
            
            // So sánh username từ token với username được yêu cầu
            if (!tokenUsername.equals(requestedUsername)) {
                log.error("Người dùng {} đang cố truy cập dữ liệu của {}", tokenUsername, requestedUsername);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập tài nguyên này");
            }
            
            log.info("Người dùng {} được phép truy cập dữ liệu của chính họ", tokenUsername);

        }catch (ResponseStatusException e) {
            throw e;
        } catch (ExpiredJwtException e) {
            log.error("JWT token đã hết hạn: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token đã hết hạn");
        } catch (UnsupportedJwtException | MalformedJwtException e) {
            log.error("JWT token không hợp lệ: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token không hợp lệ");
        } catch (Exception e) {
            log.error("Lỗi xác thực JWT: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token không hợp lệ hoặc đã hết hạn");
        }
    }

    /**
     * Trích xuất username từ JWT token
     */
    private String extractTokenUsername(String token) {
        Claims claims = extractClaims(token);
        Object rolesObj = claims.get("username");
        return claims.getSubject();
    }
    
    /**
     * Giải mã JWT token để lấy claims
     */
    private Claims extractClaims(String token) {
        try {
            // Tạo SecretKey từ Base64-encoded string
            // Theo lỗi, ta cần một khóa có độ dài ít nhất 256 bit cho HS256
            byte[] keyBytes = Base64.getDecoder().decode(secret);
            SecretKey key = Keys.hmacShaKeyFor(keyBytes);
            
            log.info("Thử giải mã với hmacShaKeyFor từ Base64 decoded secret");
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            // Fallback approach - try with raw bytes if all else fails
            try {
                log.info("Fallback: Thử với Keys.hmacShaKeyFor(secret.getBytes())");
                SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
                return Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
            } catch (Exception ex) {
                log.error("Không thể giải mã JWT: {}", ex.getMessage(), ex);
                throw ex;
            }
        }
    }

    /**
     * Trích xuất roles từ JWT token
     */
    public List<String> extractRoles(String token) {
        Claims claims = extractClaims(token);
        Object rolesObj = claims.get("roles");
        log.info("token : " + rolesObj);
        if (rolesObj instanceof List<?>) {
            return ((List<?>) rolesObj).stream().map(String::valueOf).toList();
        }

        return List.of();
    }
} 