package com.kienlongbank.classroomservice.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

@TestConfiguration
@ActiveProfiles("test")
public class WireMockConfig {

    @Value("${wiremock.server.port:9561}")
    private Integer port;

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer wireMockServer() {
        WireMockServer server = new WireMockServer(WireMockConfiguration.options().port(port));
        return server;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
} 