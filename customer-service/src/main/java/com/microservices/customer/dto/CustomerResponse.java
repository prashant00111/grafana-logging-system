package com.microservices.customer.dto;

import com.microservices.customer.entity.Customer.CustomerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String address;
    private BigDecimal walletBalance;
    private CustomerStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
