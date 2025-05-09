package com.kienlongbank.apigateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Global filter for logging API access information
 */
@Component
public class AccessLogGlobalFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger("ACCESS_LOG");
    private static final Logger errorLog = LoggerFactory.getLogger(AccessLogGlobalFilter.class);
    private static final int MAX_BODY_SIZE = 1024 * 1024; // 1MB limit
    private static final int MAX_ARRAY_ELEMENTS = 3;
    // Maximum number of array elements to show
    public static final String LIMITED_STRING = "... and %d more items (showing first %d of %d)";
    public static final String HEADER_NAME = "X-Forwarded-For";
    private final ObjectMapper objectMapper;

    /**
     * Initialize filter with ObjectMapper
     */
    public AccessLogGlobalFilter() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Format JSON content to be more readable in logs
     */
    private String formatJson(String content) {
        try {
            if (content == null || content.isEmpty()) {
                return "{}";
            }
            JsonNode jsonNode = objectMapper.readTree(content);
            
            // Limit array size if present
            limitArraySize(jsonNode);
            
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        } catch (Exception e) {
            return content; // Return original content if not valid JSON
        }
    }

    /**
     * Limit array size in JSON for more concise logging
     */
    private void limitArraySize(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<String> fieldNames = objectNode.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode fieldValue = objectNode.get(fieldName);
                if (fieldValue.isArray()) {
                    ArrayNode arrayNode = (ArrayNode) fieldValue;
                    if (arrayNode.size() > MAX_ARRAY_ELEMENTS) {
                        ArrayNode limitedArray = objectMapper.createArrayNode();
                        for (int i = 0; i < MAX_ARRAY_ELEMENTS; i++) {
                            limitedArray.add(arrayNode.get(i));
                        }
                        ObjectNode moreInfo = objectMapper.createObjectNode();
                        moreInfo.put("note", String.format(LIMITED_STRING,
                            arrayNode.size() - MAX_ARRAY_ELEMENTS, MAX_ARRAY_ELEMENTS, arrayNode.size()));
                        limitedArray.add(moreInfo);
                        objectNode.set(fieldName, limitedArray);
                    }
                } else if (fieldValue.isObject()) {
                    limitArraySize(fieldValue);
                }
            }
        }
    }

    /**
     * Format body content based on content type
     */
    private String formatBody(String bodyContent, String contentType) {
        if (contentType != null && contentType.contains(MediaType.APPLICATION_JSON_VALUE)) {
            return formatJson(bodyContent);
        }
        return bodyContent;
    }

    /**
     * Determine if request body should be logged
     */
    private boolean shouldShowRequestBody(ServerHttpRequest request) {
        return request.getMethod() != HttpMethod.GET;
    }

    /**
     * Filter and log request/response information
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        Instant startTime = Instant.now();
        
        // Capture basic request info
        String clientIp = getClientIp(request);
        String method = request.getMethod().toString();
        String path = request.getURI().getPath();
        MultiValueMap<String, String> queryParams = request.getQueryParams();
        String contentType = request.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        
        // Store request body
        AtomicReference<String> requestBody = new AtomicReference<>("");
        
        // Prepare response body capture
        AtomicReference<String> responseBody = new AtomicReference<>("");
        
        ServerHttpResponseDecorator responseDecorator = new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                return DataBufferUtils.join(Flux.from(body))
                    .flatMap(dataBuffer -> {
                        byte[] content = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(content);
                        DataBufferUtils.release(dataBuffer);
                        String bodyContent = new String(content, StandardCharsets.UTF_8);
                        String formattedContent = bodyContent;
                        if (bodyContent.length() > MAX_BODY_SIZE) {
                            formattedContent = bodyContent.substring(0, MAX_BODY_SIZE) + "... (truncated)";
                        } else {
                            formattedContent = formatBody(bodyContent, getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
                        }
                        responseBody.set(formattedContent);
                        DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
                        DataBuffer buffer = bufferFactory.wrap(content);
                        return super.writeWith(Mono.just(buffer));
                    });
            }
        };

        return DataBufferUtils.join(exchange.getRequest().getBody())
            .defaultIfEmpty(exchange.getResponse().bufferFactory().wrap(new byte[0]))
            .flatMap(dataBuffer -> {
                byte[] content = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(content);
                DataBufferUtils.release(dataBuffer);
                String bodyContent = new String(content, StandardCharsets.UTF_8);
                String formattedContent = bodyContent;
                if (bodyContent.length() > MAX_BODY_SIZE) {
                    formattedContent = bodyContent.substring(0, MAX_BODY_SIZE) + "... (truncated)";
                } else {
                    formattedContent = formatBody(bodyContent, contentType);
                }
                requestBody.set(formattedContent);
                
                ServerHttpRequest decoratedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
                    @Override
                    public Flux<DataBuffer> getBody() {
                        return Flux.just(exchange.getResponse().bufferFactory().wrap(content));
                    }
                };
                
                ServerWebExchange decoratedExchange = exchange.mutate()
                    .request(decoratedRequest)
                    .response(responseDecorator)
                    .build();

                return chain.filter(decoratedExchange)
                    .doFinally(signalType -> {
                        Duration duration = Duration.between(startTime, Instant.now());
                        int statusCode = responseDecorator.getStatusCode() != null ? 
                            responseDecorator.getStatusCode().value() : 0;
                        String responseContentType = responseDecorator.getHeaders()
                            .getFirst(HttpHeaders.CONTENT_TYPE);

                        String requestBodySection = shouldShowRequestBody(request) ?
                            String.format("""
                                . Request body (Content type: %s):
                                %s""",
                                contentType != null ? contentType : "none",
                                requestBody.get()) :
                            "";

                        log.info("""
                                
                                . Client IP: {}
                                . Method: {}
                                . Path: {}
                                . Parameters: {}
                                . Status code: {}
                                . Time: {}ms{}
                                . Response body (Content type: {}):
                                {}""",
                                clientIp,
                                method,
                                path,
                                queryParams,
                                statusCode,
                                duration.toMillis(),
                                requestBodySection,
                                responseContentType != null ? responseContentType : "none",
                                responseBody.get());
                    })
                    .doOnError(throwable -> {
                        errorLog.error("Error processing request {}: {}", 
                            path, throwable.getMessage(), throwable);
                    });
            });
    }

    /**
     * Extract client IP address from request headers or remote address
     */
    private String getClientIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst(HEADER_NAME);
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor;
        }
        return request.getRemoteAddress() != null ? 
            request.getRemoteAddress().toString() : "unknown";
    }

    /**
     * Define filter execution order
     */
    @Override
    public int getOrder() {
        return -1;
    }
} 