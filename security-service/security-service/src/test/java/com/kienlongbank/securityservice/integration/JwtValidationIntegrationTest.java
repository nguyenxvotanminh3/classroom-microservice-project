package com.kienlongbank.securityservice.integration;

import com.kienlongbank.securityservice.SecurityserviceApplication;
import com.kienlongbank.securityservice.config.TestJwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = SecurityserviceApplication.class)
@ActiveProfiles("test")
public class JwtValidationIntegrationTest {

    @Autowired
    private TestJwtUtils jwtUtils;

    private UserDetails testUserDetails;
    private String validToken;
    private String expiredToken;
    private String malformedToken = "not.a.valid.token";

    @BeforeEach
    public void setup() {
        // Tạo user test
        testUserDetails = new User(
                "testuser",
                "$2a$12$1Ec5ZKrp0DOjJceuS622Qu/2IKV0D8HF/N.7FPIF6o7qeDqyrI0Xe", // Encrypted "password123"
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // Tạo valid token
        validToken = jwtUtils.generateJwtToken(testUserDetails);

        // Tạo expired token
        expiredToken = generateExpiredToken(testUserDetails);
    }

    private String generateExpiredToken(UserDetails userDetails) {
        // Tùy chỉnh lại JwtUtils để tạo token đã hết hạn cho mục đích test
        String username = userDetails.getUsername();
        return jwtUtils.generateTokenWithCustomExpiration(username, new Date(System.currentTimeMillis() - 1000));
    }

    @Test
    public void testValidateJwtToken_ValidToken() {
        // Test validate token hợp lệ
        boolean isValid = jwtUtils.validateJwtToken(validToken);
        
        // Token hợp lệ nên kết quả phải là true
        assertTrue(isValid);
    }

    @Test
    public void testValidateJwtToken_ExpiredToken() {
        // Test validate token đã hết hạn
        boolean isValid = jwtUtils.validateJwtToken(expiredToken);
        
        // Token đã hết hạn nên kết quả phải là false
        assertFalse(isValid);
    }

    @Test
    public void testValidateJwtToken_MalformedToken() {
        // Test validate token không đúng định dạng
        boolean isValid = jwtUtils.validateJwtToken(malformedToken);
        
        // Token không đúng định dạng nên kết quả phải là false
        assertFalse(isValid);
    }

    @Test
    public void testGetUserNameFromJwtToken_ValidToken() {
        // Test lấy username từ token hợp lệ
        String username = jwtUtils.getUserNameFromJwtToken(validToken);
        
        // Username trích xuất từ token phải khớp với username đã dùng để tạo token
        assertEquals("testuser", username);
    }

    @Test
    public void testGetUserNameFromJwtToken_ExpiredToken() {
        // Lưu ý: Mặc dù token đã hết hạn, nhưng phương thức getUserNameFromJwtToken 
        // vẫn có thể trích xuất được username, vì nó không kiểm tra thời hạn
        String username = jwtUtils.getUserNameFromJwtToken(expiredToken);
        
        assertEquals("testuser", username);
    }

    @Test
    public void testGetUserNameFromJwtToken_MalformedToken() {
        // Khi token sai định dạng, phương thức getUserNameFromJwtToken có thể ném exception
        // hoặc trả về null tùy thuộc vào cài đặt
        try {
            String username = jwtUtils.getUserNameFromJwtToken(malformedToken);
            assertNull(username); // Nếu không ném exception, thì kết quả phải là null
        } catch (Exception e) {
            // Nếu ném exception, điều này cũng là hợp lý
            assertTrue(e instanceof RuntimeException || e instanceof io.jsonwebtoken.JwtException);
        }
    }
} 