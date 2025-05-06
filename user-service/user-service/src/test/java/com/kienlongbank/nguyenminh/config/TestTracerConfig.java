package com.kienlongbank.nguyenminh.config;

import io.micrometer.observation.ObservationRegistry;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestTracerConfig {
    
    @Bean
    @Primary
    public Tracer tracer() {
        return mock(Tracer.class);
    }

    @Bean
    @Primary
    public ObservationRegistry observationRegistry() {
        return ObservationRegistry.NOOP;
    }
} 