package com.microservices.order.service;

import com.microservices.order.dto.OrderRequest;
import com.microservices.order.dto.OrderResponse;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(OrderRequest request);

    OrderResponse getOrderById(Long id);

    OrderResponse getOrderByOrderNumber(String orderNumber);

    List<OrderResponse> getOrdersByCustomerId(Long customerId);

    List<OrderResponse> getAllOrders();

    OrderResponse cancelOrder(Long id);
}
