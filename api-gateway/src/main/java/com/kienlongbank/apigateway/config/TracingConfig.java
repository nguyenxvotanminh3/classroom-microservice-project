package com.kienlongbank.apigateway.config;

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.server.WebFilter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;

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

    @Bean
    public WebClient.Builder webClientBuilder(ObservationRegistry observationRegistry) {
        return WebClient.builder()
            .filter(createTracingExchangeFilter(observationRegistry));
    }
    
    private ExchangeFilterFunction createTracingExchangeFilter(ObservationRegistry observationRegistry) {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            Span currentSpan = Span.current();
            return Mono.just(ClientRequest.from(request)
                .header("traceparent", getTraceparentHeader(currentSpan))
                .build());
        });
    }

    private String getTraceparentHeader(Span span) {
        if (span == null || !span.getSpanContext().isValid()) {
            return "";
        }
        return span.getSpanContext().getTraceId() + "-" + span.getSpanContext().getSpanId();
    }

    @Bean
    public WebFilter tracingWebFilter(Tracer tracer) {
        return (exchange, chain) -> {
            Span serverSpan = tracer.spanBuilder("gateway.request")
                .setAttribute("http.method", exchange.getRequest().getMethod().name())
                .setAttribute("http.url", exchange.getRequest().getURI().toString())
                .startSpan();

            return Mono.using(
                () -> serverSpan.makeCurrent(),
                scope -> chain.filter(exchange)
                    .doOnSuccess(v -> serverSpan.setStatus(io.opentelemetry.api.trace.StatusCode.OK))
                    .doOnError(e -> {
                        serverSpan.recordException(e);
                        serverSpan.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR);
                    }),
                scope -> {
                    serverSpan.end();
                    scope.close();
                }
            );
        };
    }
}