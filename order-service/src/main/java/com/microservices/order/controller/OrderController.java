package com.microservices.order.controller;

import com.microservices.order.dto.ApiResponse;
import com.microservices.order.dto.OrderRequest;
import com.microservices.order.dto.OrderResponse;
import com.microservices.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "Endpoints for creating and managing orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create and process a new order")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody OrderRequest request) {
        log.info("REST request to create order for customer ID: {}", request.getCustomerId());
        OrderResponse response = orderService.createOrder(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location)
                .body(ApiResponse.success(response, "Order created and processed successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        log.info("REST request to get order by ID: {}", id);
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Get order by order number")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByOrderNumber(@PathVariable String orderNumber) {
        log.info("REST request to get order by order number: {}", orderNumber);
        OrderResponse response = orderService.getOrderByOrderNumber(orderNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get orders by customer ID")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByCustomerId(@PathVariable Long customerId) {
        log.info("REST request to get orders for customer ID: {}", customerId);
        List<OrderResponse> responses = orderService.getOrdersByCustomerId(customerId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping
    @Operation(summary = "List all orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders() {
        log.info("REST request to list all orders");
        List<OrderResponse> responses = orderService.getAllOrders();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable Long id) {
        log.info("REST request to cancel order ID: {}", id);
        OrderResponse response = orderService.cancelOrder(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Order cancelled successfully"));
    }
}
