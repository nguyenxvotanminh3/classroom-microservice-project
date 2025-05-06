package com.kienlongbank.apigateway.config;


import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tracing is auto-configured by Spring Boot with the presence of micrometer-tracing-bridge-otel
 * No additional configuration is needed
 */
@Configuration
public class TracingConfig {

    @Bean
    public OtlpGrpcSpanExporter otlpGrpcSpanExporter(@Value("${otel.exporter.otlp.endpoint}") String endpoint) {
        // For gRPC, the endpoint should not include the /v1/traces path
        return OtlpGrpcSpanExporter.builder()
                .setEndpoint(endpoint)
                .build();
    }
}