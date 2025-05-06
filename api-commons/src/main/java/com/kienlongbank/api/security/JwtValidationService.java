package com.kienlongbank.api.security;

import java.util.List;

/**
 * Dubbo interface để xác thực JWT/JWE token và lấy thông tin người dùng
 */
public interface JwtValidationService {
    
    /**
     * Xác thực token và kiểm tra xem token thuộc về username không
     * @param token JWT/JWE token
     * @param requestedUsername Username cần kiểm tra
     * @return true nếu token hợp lệ và thuộc về username, false nếu ngược lại
     */
    boolean validateTokenForUsername(String token, String requestedUsername);
    
    /**
     * Lấy username từ token
     * @param token JWT/JWE token
     * @return username nếu token hợp lệ, null nếu token không hợp lệ
     */
    String getUsernameFromToken(String token);

    /**
     * Lấy role từ token
     * @param token JWT/JWE token
     * @return role nếu token hợp lệ, null nếu token không hợp lệ
     */
    String getRoleFromToken(String token);

    /**
     * Kiểm tra token có hợp lệ không
     * @param token JWT/JWE token
     * @return true nếu token hợp lệ, false nếu ngược lại
     */
    boolean validateToken(String token);

    List<String> extractRoles(String token);

    boolean hasRole(String token, String role);

    boolean hasAnyRole(String token, List<String> roles);
}