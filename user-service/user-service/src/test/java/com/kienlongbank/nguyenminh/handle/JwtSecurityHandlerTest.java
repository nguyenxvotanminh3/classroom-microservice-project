package com.kienlongbank.nguyenminh.handle;

import java.util.Base64;
import java.util.Date;
import java.util.List;



import com.kienlongbank.nguyenminh.user.handler.JwtSecurityHandler;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JwtSecurityHandlerTest {

    private JwtSecurityHandler jwtSecurityHandler;
    private HttpServletRequest mockRequest;
    private String secret = Base64.getEncoder().encodeToString("mySuperSecretKey1234567890123456".getBytes()); // 256-bit

    @BeforeEach
    public void setup() {
        jwtSecurityHandler = new JwtSecurityHandler();
        mockRequest = mock(HttpServletRequest.class);
        ReflectionTestUtils.setField(jwtSecurityHandler, "secret", secret);
    }

    private String createToken(String subject, long expirationMillis) {
        return Jwts.builder()
                .setSubject(subject)
                .claim("username", subject)
                .claim("roles", List.of("ROLE_USER"))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(SignatureAlgorithm.HS256, Base64.getDecoder().decode(secret))
                .compact();
    }

    @Test
    public void testValidateUserAccess_success() {
        String username = "testuser";
        String token = createToken(username, 10000);
        when(mockRequest.getHeader("Authorization")).thenReturn("Bearer " + token);

        assertDoesNotThrow(() -> jwtSecurityHandler.validateUserAccess(mockRequest, username));
    }

    @Test
    public void testValidateUserAccess_invalidUser() {
        String token = createToken("userA", 10000);
        when(mockRequest.getHeader("Authorization")).thenReturn("Bearer " + token);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> jwtSecurityHandler.validateUserAccess(mockRequest, "userB"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    public void testValidateUserAccess_noToken() {
        when(mockRequest.getHeader("Authorization")).thenReturn(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> jwtSecurityHandler.validateUserAccess(mockRequest, "user"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    public void testValidateUserAccess_expiredToken() {
        String token = createToken("testuser", -10000); // token đã hết hạn
        when(mockRequest.getHeader("Authorization")).thenReturn("Bearer " + token);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> jwtSecurityHandler.validateUserAccess(mockRequest, "testuser"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    public void testExtractRoles() {
        String token = createToken("testuser", 10000);
        List<String> roles = jwtSecurityHandler.extractRoles(token);

        assertEquals(List.of("ROLE_USER"), roles);
    }
}
