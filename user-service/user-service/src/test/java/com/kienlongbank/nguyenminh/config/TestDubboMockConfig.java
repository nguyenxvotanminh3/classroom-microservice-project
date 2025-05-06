package com.kienlongbank.nguyenminh.config;

import com.kienlongbank.api.SecurityService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestDubboMockConfig {
    @Bean
    public SecurityService securityService() {
        return Mockito.mock(SecurityService.class);
    }
} 