package com.microservices.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Slf4j
@Component
public class LoggingGlobalFilter implements GlobalFilter, Ordered {

    private static final String START_TIME = "startTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        exchange.getAttributes().put(START_TIME, System.currentTimeMillis());

        String correlationId = request.getHeaders().getFirst(CorrelationIdGlobalFilter.CORRELATION_ID_HEADER);
        String clientIp = getClientIp(request);

        log.info("Incoming Request -> CorrelationId: [{}], Method: [{}], URI: [{}], Client IP: [{}]",
                correlationId, request.getMethod(), request.getURI().getPath(), clientIp);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            Long startTime = exchange.getAttribute(START_TIME);
            long duration = (startTime != null) ? (System.currentTimeMillis() - startTime) : 0;
            HttpStatusCode statusCode = exchange.getResponse().getStatusCode();

            log.info("Outgoing Response <- CorrelationId: [{}], Method: [{}], URI: [{}], Status: [{}], Latency: [{} ms]",
                    correlationId, request.getMethod(), request.getURI().getPath(), statusCode, duration);
        }));
    }

    private String getClientIp(ServerHttpRequest request) {
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "UNKNOWN";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
