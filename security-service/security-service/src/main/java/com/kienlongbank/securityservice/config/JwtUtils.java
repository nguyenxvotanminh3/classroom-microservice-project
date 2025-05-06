package com.kienlongbank.securityservice.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private final JwtEncryptionConfig jwtEncryptionConfig;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secret.trim());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        try {
            // If token starts with "Bearer ", remove it
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
//            // First decrypt JWE to JWT
//            String decryptedJwt = jwtEncryptionConfig.decryptJweToJwt(token);
            return extractClaim(token, Claims::getSubject);
        } catch (Exception e) {
            log.error("Error extracting username: " + e.getMessage(), e);
            return null;
        }
    }

    public Date extractExpiration(String token) {
        try {
            // If token starts with "Bearer ", remove it
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            // First decrypt JWE to JWT
            String decryptedJwt = jwtEncryptionConfig.decryptJweToJwt(token);
            return extractClaim(decryptedJwt, Claims::getExpiration);
        } catch (Exception e) {
            log.error("Error extracting expiration: " + e.getMessage(), e);
            return new Date(0); // Return a date in the past
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("Error extracting claims from token: " + e.getMessage());
            throw e;
        }
    }

    private Boolean isTokenExpired(String token) {
        try {
            // If token starts with "Bearer ", remove it
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            // First decrypt JWE to JWT
            String decryptedJwt = jwtEncryptionConfig.decryptJweToJwt(token);
            return extractExpiration(decryptedJwt).before(new Date());
        } catch (Exception e) {
            log.error("Error checking token expiration: " + e.getMessage());
            return true;
        }
    }

    public String generateJwtToken(UserDetails userDetails) throws Exception {
        Map<String, Object> claims = new HashMap<>();
        // List from authorities
        List<String> roles = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());
        // Put role and username into claim
        claims.put("roles", roles);
        claims.put("username", userDetails.getUsername());

        try {
            return "Bearer " + createToken(claims, userDetails.getUsername());
        } catch (Exception e) {
            log.error("Error generating token: " + e.getMessage());
            throw e;
        }
    }

    private String createToken(Map<String, Object> claims, String subject) throws Exception {
        try {
            log.info("Creating token for subject: {}", subject);
            String jwtToken = Jwts.builder()
                    .setClaims(claims)   // claims chứa roles
                    .setSubject(subject) // subject là username
                    .setIssuedAt(new Date(System.currentTimeMillis()))
                    .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
                    .signWith(getSigningKey(), io.jsonwebtoken.SignatureAlgorithm.HS256)
                    .compact();
            
            log.debug("JWT created, now encrypting as JWE");
            return jwtEncryptionConfig.encryptJwtAsJwe(jwtToken);
        } catch (Exception e) {
            log.error("Error creating token: " + e.getMessage());
            throw e;
        }
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            // If token starts with "Bearer ", remove it
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            // First decrypt JWE to JWT
            String decryptedJwt = jwtEncryptionConfig.decryptJweToJwt(token);
            
            // Extract username from decrypted JWT
            final String username = extractClaim(decryptedJwt, Claims::getSubject);
            
            // Check if username matches and token is not expired
            return username.equals(userDetails.getUsername()) && 
                   !extractClaim(decryptedJwt, claims -> claims.getExpiration().before(new Date()));
        } catch (Exception e) {
            log.error("Error validating token: " + e.getMessage());
            return false;
        }
    }
}
