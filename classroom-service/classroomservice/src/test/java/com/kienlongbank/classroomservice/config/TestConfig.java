package com.kienlongbank.classroomservice.config;

import com.kienlongbank.api.SecurityService;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
@EnableDubbo(scanBasePackages = {})
public class TestConfig {

    @Bean
    @Primary
    public ApplicationConfig applicationConfig() {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("classroom-service-test");
        applicationConfig.setQosEnable(false);
        return applicationConfig;
    }

    @Bean
    @Primary
    public RegistryConfig registryConfig() {
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress("N/A");
        registryConfig.setRegister(false);
        registryConfig.setCheck(false);
        return registryConfig;
    }

    @Bean
    @Primary
    public SecurityService securityService() {
        return Mockito.mock(SecurityService.class);
    }
} 