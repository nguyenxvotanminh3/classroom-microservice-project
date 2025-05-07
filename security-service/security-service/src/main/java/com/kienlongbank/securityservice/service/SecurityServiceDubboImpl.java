package com.kienlongbank.securityservice.service;

import com.kienlongbank.api.SecurityService;
import com.kienlongbank.securityservice.config.JwtEncryptionConfig;
import com.kienlongbank.securityservice.config.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;

import java.util.*;

@Service
@DubboService(version = "1.0.0", group = "security")
@Slf4j
@RequiredArgsConstructor
public class SecurityServiceDubboImpl implements SecurityService {
    private final JwtUtils jwtUtils;
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private final JwtEncryptionConfig jwtEncryptionConfig;
    
    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public Map<String, Object> authenticate(String username, String password) {
        Map<String, Object> result = new HashMap<>();
        
        // Implement actual authentication logic here or call your existing authentication service
        log.info("Authenticating user: {}", username);
        
        // For demonstration purposes only, in a real implementation you would validate against your user store
        if ("admin".equals(username) && "password".equals(password)) {
            String token = generateToken(username);
            result.put("token", token);
            result.put("username", username);
            result.put("status", "success");
        } else {
            result.put("status", "error");
            result.put("message", "Invalid credentials");
        }
        
        return result;
    }

    @Override
    public boolean validateToken(String token) {
        try {
            log.debug("Validating token starting with: {}", token.substring(0, Math.min(20, token.length())));
            
            // If token starts with "Bearer ", remove it
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            try {
                // Step 1: Decrypt JWE to JWT
                String decryptedJwt = jwtEncryptionConfig.decryptJweToJwt(token);
                log.debug("Successfully decrypted JWE token");
                
                // Step 2: Parse and validate JWT
                final Claims claims = getClaimsFromToken(decryptedJwt);
                log.debug("Successfully parsed JWT claims");
                
                // Step 3: Check if token is expired
                final Date expirationDate = claims.getExpiration();
                boolean isValid = expirationDate.after(new Date());
                log.debug("Token valid? {}, expires at: {}", isValid, expirationDate);
                return isValid;
            } catch (Exception e) {
                // If JWE decryption fails, try treating it as a plain JWT
                log.debug("JWE decryption failed, trying as plain JWT");
                final Claims claims = getClaimsFromToken(jwtEncryptionConfig.decryptJweToJwt(token));
                final Date expirationDate = claims.getExpiration();
                return expirationDate.after(new Date());
            }
        } catch (Exception e) {
            log.error("Error validating token", e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getUserDetailsFromToken(String token) {
        Map<String, Object> result = new HashMap<>();
        try {
            // If token starts with "Bearer ", remove it
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            String decryptedJwt = jwtEncryptionConfig.decryptJweToJwt(token);
            final Claims claims = getClaimsFromToken(decryptedJwt);
            
            result.put("username", claims.getSubject());
            result.put("expiration", claims.getExpiration());
            result.put("issuedAt", claims.getIssuedAt());
            
            // Add any additional claims that you store in the token
            if (claims.get("roles") != null) {
                result.put("roles", claims.get("roles"));
            }
            
            return result;
        } catch (Exception e) {
            log.error("Error extracting user details from token", e);
            result.put("error", "Invalid token");
            return result;
        }
    }

    @Override
    public List<String> extractRoles(String token) {
        try {
            // If token starts with "Bearer ", remove it
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            String decryptedJwt = jwtEncryptionConfig.decryptJweToJwt(token);
            final Claims claims = getClaimsFromToken(decryptedJwt);
            
            return (List<String>) claims.get("roles");
        } catch (Exception e) {
            log.error("Error extracting roles from token", e);
            return List.of();
        }
    }

    @Override
    public boolean hasRole(String token, String role) {
        List<String> roles = extractRoles(token);
        return roles != null && roles.contains(role);
    }

    @Override
    public boolean hasAnyRole(String token, List<String> roles) {
        List<String> userRoles = extractRoles(token);
        if (userRoles == null || roles == null) {
            return false;
        }
        
        return userRoles.stream().anyMatch(roles::contains);
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


    private String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims getClaimsFromToken(String token) throws Exception {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("Failed to parse JWT token", e);
            throw e;
        }
    }

    @Override
    public boolean validateTokenForUsername(String token, String requestedUsername) {
        try {
            log.info("token : " +token);
            // Giải mã JWE nếu cần
            String decryptedJwt = jwtEncryptionConfig.decryptJweToJwt(token);
            Claims claims = getClaimsFromToken(decryptedJwt);

            // Lấy username từ cả subject và trường "username" trong claims
            String usernameInToken = claims.getSubject();
            Object usernameClaimObj = claims.get("username");
            String usernameClaim = usernameClaimObj != null ? usernameClaimObj.toString() : null;

            Date expirationDate = claims.getExpiration();

            boolean valid = requestedUsername != null
                    && (requestedUsername.equals(usernameInToken) || requestedUsername.equals(usernameClaim))
                    && expirationDate != null && expirationDate.after(new Date());

            log.debug("validateTokenForUsername: token subject={}, claim.username={}, requestedUsername={}, valid={}",
                    usernameInToken, usernameClaim, requestedUsername, valid);

            return valid;
        } catch (Exception e) {
            log.error("Error validating token for username", e);
            return false;
        }
    }


} 