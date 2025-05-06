package com.kienlongbank.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AccessLogGlobalFilter implements GlobalFilter, Ordered {
    private static final Logger accessLogger = LoggerFactory.getLogger("ACCESS_LOG");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        StringBuilder msg = new StringBuilder();
        msg.append("REQUEST DATA : ")
           .append("method=").append(request.getMethod())
           .append(", uri=").append(request.getURI())
           .append(", clientIp=").append(request.getRemoteAddress())
           .append(", headers=").append(request.getHeaders());
        accessLogger.info(msg.toString());
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1; // Ưu tiên cao
    }
} 