package com.microservices.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class CorrelationIdGlobalFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
            log.debug("Generated new Correlation-ID: {}", correlationId);
        } else {
            log.debug("Found existing Correlation-ID: {}", correlationId);
        }

        final String finalCorrelationId = correlationId;

        // Mutate request with the correlation ID header
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(CORRELATION_ID_HEADER, finalCorrelationId)
                .build();

        // Add correlation ID header to the HTTP response
        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, finalCorrelationId);

        MDC.put("correlationId", finalCorrelationId);

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .contextWrite(context -> context.put("correlationId", finalCorrelationId))
                .doFinally(signalType -> MDC.remove("correlationId"));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
