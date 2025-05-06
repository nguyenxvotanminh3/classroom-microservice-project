package com.kienlongbank.classroomservice.config;

import com.kienlongbank.api.SecurityService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class SecurityServiceMockConfig {
    
    @Bean
    @Primary
    public SecurityService securityService() {
        return Mockito.mock(SecurityService.class);
    }
} 