package com.microservices.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private Mono<ResponseEntity<Map<String, Object>>> buildFallbackResponse(String serviceName) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("error", "Service Unavailable");
        response.put("message", serviceName + " is currently unavailable or taking too long to respond. Please try again later.");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @GetMapping("/customers")
    public Mono<ResponseEntity<Map<String, Object>>> customerFallback() {
        return buildFallbackResponse("Customer Service");
    }

    @GetMapping("/products")
    public Mono<ResponseEntity<Map<String, Object>>> productFallback() {
        return buildFallbackResponse("Product Service");
    }

    @GetMapping("/orders")
    public Mono<ResponseEntity<Map<String, Object>>> orderFallback() {
        return buildFallbackResponse("Order Service");
    }

    @GetMapping("/payments")
    public Mono<ResponseEntity<Map<String, Object>>> paymentFallback() {
        return buildFallbackResponse("Payment Service");
    }
}
