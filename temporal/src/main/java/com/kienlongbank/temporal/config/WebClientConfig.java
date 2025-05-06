package com.kienlongbank.temporal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${security-service.url:http://localhost:8081}")
    private String securityServiceUrl;

    @Value("${classroom-service.url:http://localhost:8082}")
    private String classroomServiceUrl;
    
    @Value("${email-service.url:http://localhost:8083}")
    private String emailServiceUrl;

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient securityServiceWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .baseUrl(securityServiceUrl)
                .build();
    }
    
    @Bean
    public WebClient classroomServiceWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .baseUrl(classroomServiceUrl)
                .build();
    }
    
    @Bean
    public WebClient emailServiceWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .baseUrl(emailServiceUrl)
                .build();
    }
} 