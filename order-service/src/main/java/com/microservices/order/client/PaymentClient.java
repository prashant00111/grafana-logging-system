package com.microservices.order.client;

import com.microservices.order.client.dto.PaymentRequestDto;
import com.microservices.order.client.dto.PaymentResponseDto;
import com.microservices.order.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service")
public interface PaymentClient {

    @PostMapping("/api/v1/payments/process")
    ApiResponse<PaymentResponseDto> processPayment(@RequestBody PaymentRequestDto request);
}
