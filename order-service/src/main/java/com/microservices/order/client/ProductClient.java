package com.microservices.order.client;

import com.microservices.order.client.dto.ProductDto;
import com.microservices.order.client.dto.StockUpdateDto;
import com.microservices.order.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/api/v1/products/{id}")
    ApiResponse<ProductDto> getProductById(@PathVariable("id") Long id);

    @PostMapping("/api/v1/products/{id}/deduct-stock")
    ApiResponse<ProductDto> deductStock(@PathVariable("id") Long id, @RequestBody StockUpdateDto request);

    @PostMapping("/api/v1/products/{id}/restore-stock")
    ApiResponse<ProductDto> restoreStock(@PathVariable("id") Long id, @RequestBody StockUpdateDto request);
}
