package com.kienlongbank.securityservice.config;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Component
public class JwtEncryptionConfig {

    @Value("${jwt.encryption.key}")
    private String encryptionKey;

    public String encryptJwtAsJwe(String signedJwt) throws Exception {
        try {
            // Create payload from signed JWT
            Payload payload = new Payload(signedJwt);
            
            // Create JWE Header
            JWEHeader header = new JWEHeader.Builder(JWEAlgorithm.DIR, EncryptionMethod.A192GCM)
                    .contentType("JWT")
                    .build();
                    
            // Create JWE Object
            JWEObject jweObject = new JWEObject(header, payload);
            
            // Convert key string from config to AES key
            byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
            if (keyBytes.length != 24) {
                log.warn("Encryption key length is {}, expected 24 bytes for A192GCM", keyBytes.length);
            }
            
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            
            // Encrypt
            DirectEncrypter encrypter = new DirectEncrypter(secretKey);
            jweObject.encrypt(encrypter);
            
            String result = jweObject.serialize();
            log.debug("Successfully encrypted JWT to JWE");
            return result;
        } catch (Exception e) {
            log.error("Error encrypting JWT: {}", e.getMessage(), e);
            throw e;
        }
    }

    public String decryptJweToJwt(String jweString) throws Exception {
        try {
            // Parse JWE string to JWEObject
            JWEObject jweObject = JWEObject.parse(jweString);
            
            // Create AES key from key string
            byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            
            // Decrypt
            DirectDecrypter decrypter = new DirectDecrypter(secretKey);
            jweObject.decrypt(decrypter);
            
            // Get payload inside - it's the JWT string
            String jwt = jweObject.getPayload().toSignedJWT().serialize();
            log.debug("Successfully decrypted JWE to JWT");
            return jwt;
        } catch (Exception e) {
            log.error("Error decrypting JWE: {}", e.getMessage(), e);
            throw e;
        }
    }
}
