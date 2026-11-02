package com.microservices.payment.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Slf4j
@Configuration
public class FeignCorrelationInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
            log.debug("No Correlation-ID in MDC; generated new one for Feign call: {}", correlationId);
        }
        template.header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId);
    }
}
