package com.kienlongbank.nguyenminh.config;

import io.micrometer.tracing.Tracer;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestTracerConfig {
    @Bean
    public Tracer tracer() {
        return Mockito.mock(Tracer.class, Mockito.RETURNS_DEEP_STUBS);
    }
} 