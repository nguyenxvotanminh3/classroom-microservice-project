package com.kienlongbank.securityservice.api;

import java.util.List;

/**
 * Service for JWT validation
 */
public interface JwtValidationService {
    
    /**
     * Xác thực JWT token
     * 
     * @param token JWT token cần xác thực
     * @return true nếu token hợp lệ, false nếu không
     */
    boolean validateToken(String token);
    
    /**
     * Lấy username từ JWT token
     * 
     * @param token JWT token
     * @return Username hoặc null nếu token không hợp lệ
     */
    String getUsernameFromToken(String token);
    
    /**
     * Kiểm tra xem token có hợp lệ và thuộc về username cụ thể không
     * 
     * @param token JWT token cần xác thực
     * @param username Username cần kiểm tra
     * @return true nếu token hợp lệ và thuộc về username, false nếu không
     */
    boolean validateTokenForUsername(String token, String username);
    
    /**
     * Trích xuất roles từ JWT token
     * 
     * @param token JWT token
     * @return Danh sách các role hoặc danh sách trống nếu token không hợp lệ
     */
    List<String> extractRoles(String token);
    
    /**
     * Kiểm tra xem token có chứa role được yêu cầu không
     * 
     * @param token JWT token cần kiểm tra
     * @param role Role cần kiểm tra
     * @return true nếu token chứa role được yêu cầu
     */
    boolean hasRole(String token, String role);
    
    /**
     * Kiểm tra xem token có chứa bất kỳ role nào trong danh sách được yêu cầu không
     * 
     * @param token JWT token cần kiểm tra
     * @param roles Danh sách các role cần kiểm tra
     * @return true nếu token chứa ít nhất một trong các role được yêu cầu
     */
    boolean hasAnyRole(String token, List<String> roles);
} 