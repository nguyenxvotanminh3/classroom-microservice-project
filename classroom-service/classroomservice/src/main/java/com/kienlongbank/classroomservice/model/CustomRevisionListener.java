package com.kienlongbank.classroomservice.model;

import com.kienlongbank.api.SecurityService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.dubbo.config.annotation.DubboReference;
import org.hibernate.envers.RevisionListener;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Component
@Slf4j
public class CustomRevisionListener implements RevisionListener {

    @DubboReference(version = "1.0.0", group = "security", check = false)
    private SecurityService securityService;

    @Override
    public void newRevision(Object revisionEntity) {
        CustomRevisionEntity customRevisionEntity = (CustomRevisionEntity) revisionEntity;
        
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    log.debug("Extracting username from token: {}", token.substring(0, Math.min(20, token.length())));
                    
                    // Sử dụng getUserDetailsFromToken thay vì getUsernameFromToken
                    Map<String, Object> userDetails = securityService.getUserDetailsFromToken(token);
                    
                    if (userDetails != null && userDetails.containsKey("username")) {
                        String username = userDetails.get("username").toString();
                        log.info("Extracted username for audit: {}", username);
                        customRevisionEntity.setUsername(username);
                    } else {
                        log.warn("Could not extract username from token, user details: {}", userDetails);
                        customRevisionEntity.setUsername("anonymous");
                    }
                } catch (Exception e) {
                    log.error("Error extracting username from token: {}", e.getMessage());
                    customRevisionEntity.setUsername("error");
                }
            } else {
                log.debug("No Authorization header found or not a Bearer token");
                customRevisionEntity.setUsername("anonymous");
            }
        } else {
            log.debug("No request context found");
            customRevisionEntity.setUsername("system");
        }
    }
    
    private HttpServletRequest getCurrentRequest() {
        try {
            return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        } catch (IllegalStateException e) {
            log.debug("Could not access current request: {}", e.getMessage());
            return null;
        }
    }
} 