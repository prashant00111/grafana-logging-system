package com.microservices.order.client;

import com.microservices.order.client.dto.CustomerDto;
import com.microservices.order.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service")
public interface CustomerClient {

    @GetMapping("/api/v1/customers/{id}")
    ApiResponse<CustomerDto> getCustomerById(@PathVariable("id") Long id);
}
