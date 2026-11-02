package com.microservices.order.dto;

import com.microservices.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private String orderNumber;
    private Long customerId;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private String paymentReference;
    private String failureReason;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
