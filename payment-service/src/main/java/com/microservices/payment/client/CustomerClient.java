package com.microservices.payment.client;

import com.microservices.payment.client.dto.WalletTransactionRequestDto;
import com.microservices.payment.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "customer-service")
public interface CustomerClient {

    @PostMapping("/api/v1/customers/{id}/wallet/deduct")
    ApiResponse<Object> deductFromWallet(@PathVariable("id") Long id, @RequestBody WalletTransactionRequestDto request);

    @PostMapping("/api/v1/customers/{id}/wallet/deposit")
    ApiResponse<Object> depositToWallet(@PathVariable("id") Long id, @RequestBody WalletTransactionRequestDto request);
}
