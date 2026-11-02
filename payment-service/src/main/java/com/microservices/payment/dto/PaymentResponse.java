package com.microservices.payment.dto;

import com.microservices.payment.entity.PaymentMethod;
import com.microservices.payment.entity.PaymentStatus;
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
public class PaymentResponse {

    private Long id;
    private String paymentReference;
    private String orderNumber;
    private Long customerId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
