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
public class PaymentRequestDto {
    private String orderNumber;
    private Long customerId;
    private BigDecimal amount;
    private String paymentMethod;
}
