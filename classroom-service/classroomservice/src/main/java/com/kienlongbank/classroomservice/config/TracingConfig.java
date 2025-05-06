package com.kienlongbank.classroomservice.config;

import org.springframework.context.annotation.Configuration;

/**
 * Tracing is auto-configured by Spring Boot with the presence of micrometer-tracing-bridge-otel
 * No additional configuration is needed
 */
@Configuration
public class TracingConfig {
    // Empty configuration class
    // Spring Boot auto-configures tracing with the appropriate dependencies on the classpath
} 