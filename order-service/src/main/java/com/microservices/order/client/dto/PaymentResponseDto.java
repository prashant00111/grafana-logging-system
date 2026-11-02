package com.microservices.order.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {
    private Long id;
    private String paymentReference;
    private String orderNumber;
    private Long customerId;
    private BigDecimal amount;
    private String status;
    private String paymentMethod;
}
