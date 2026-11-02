package com.microservices.order.service.impl;

import com.microservices.order.client.CustomerClient;
import com.microservices.order.client.PaymentClient;
import com.microservices.order.client.ProductClient;
import com.microservices.order.client.dto.*;
import com.microservices.order.dto.ApiResponse;
import com.microservices.order.dto.OrderItemRequest;
import com.microservices.order.dto.OrderItemResponse;
import com.microservices.order.dto.OrderRequest;
import com.microservices.order.dto.OrderResponse;
import com.microservices.order.entity.Order;
import com.microservices.order.entity.OrderItem;
import com.microservices.order.entity.OrderStatus;
import com.microservices.order.exception.OrderNotFoundException;
import com.microservices.order.exception.OrderProcessingException;
import com.microservices.order.repository.OrderRepository;
import com.microservices.order.service.OrderService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CustomerClient customerClient;
    private final ProductClient productClient;
    private final PaymentClient paymentClient;
    private final Counter orderCreatedCounter;
    private final Counter orderFailedCounter;

    public OrderServiceImpl(OrderRepository orderRepository,
                            CustomerClient customerClient,
                            ProductClient productClient,
                            PaymentClient paymentClient,
                            MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.customerClient = customerClient;
        this.productClient = productClient;
        this.paymentClient = paymentClient;
        this.orderCreatedCounter = Counter.builder("orders.created.count")
                .description("Number of orders successfully created and paid")
                .register(meterRegistry);
        this.orderFailedCounter = Counter.builder("orders.failed.count")
                .description("Number of orders that failed during processing")
                .register(meterRegistry);
    }

    @Override
    public OrderResponse createOrder(OrderRequest request) {
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Initiating order creation [orderNumber: {}] for customerId: {}", orderNumber, request.getCustomerId());

        // 1. Verify Customer
        verifyCustomer(request.getCustomerId());

        // 2. Validate products and reserve stock
        List<OrderItem> items = new ArrayList<>();
        List<StockUpdateDto> stockCompensations = new ArrayList<>();
        List<Long> compensatedProductIds = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        try {
            for (OrderItemRequest itemReq : request.getItems()) {
                ApiResponse<ProductDto> productResp = productClient.getProductById(itemReq.getProductId());
                if (productResp == null || productResp.getData() == null) {
                    throw new OrderProcessingException("Product not found with id: " + itemReq.getProductId());
                }
                ProductDto product = productResp.getData();

                if (product.getStockQuantity() < itemReq.getQuantity()) {
                    throw new OrderProcessingException(String.format("Insufficient stock for product %s. Available: %d, Requested: %d",
                            product.getName(), product.getStockQuantity(), itemReq.getQuantity()));
                }

                // Deduct stock
                StockUpdateDto deductReq = StockUpdateDto.builder()
                        .quantity(itemReq.getQuantity())
                        .reason("Order " + orderNumber)
                        .build();
                productClient.deductStock(product.getId(), deductReq);
                stockCompensations.add(deductReq);
                compensatedProductIds.add(product.getId());

                BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
                totalAmount = totalAmount.add(subtotal);

                OrderItem orderItem = OrderItem.builder()
                        .productId(product.getId())
                        .productSku(product.getSku())
                        .productName(product.getName())
                        .quantity(itemReq.getQuantity())
                        .unitPrice(product.getPrice())
                        .subtotal(subtotal)
                        .build();
                items.add(orderItem);
            }
        } catch (Exception ex) {
            log.error("Failed during stock reservation. Rolling back reserved items for order {}", orderNumber, ex);
            rollbackStock(compensatedProductIds, stockCompensations);
            orderFailedCounter.increment();
            throw new OrderProcessingException("Order failed during stock reservation: " + ex.getMessage(), ex);
        }

        // 3. Create initial order record in PENDING status
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .customerId(request.getCustomerId())
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .build();

        for (OrderItem item : items) {
            order.addItem(item);
        }
        Order savedOrder = orderRepository.save(order);

        // 4. Process Payment
        try {
            log.info("Processing payment of {} for order: {}", totalAmount, orderNumber);
            PaymentRequestDto paymentReq = PaymentRequestDto.builder()
                    .orderNumber(orderNumber)
                    .customerId(request.getCustomerId())
                    .amount(totalAmount)
                    .paymentMethod(request.getPaymentMethod())
                    .build();

            ApiResponse<PaymentResponseDto> paymentResp = paymentClient.processPayment(paymentReq);

            if (paymentResp != null && paymentResp.getData() != null && "SUCCESS".equalsIgnoreCase(paymentResp.getData().getStatus())) {
                savedOrder.setStatus(OrderStatus.PAID);
                savedOrder.setPaymentReference(paymentResp.getData().getPaymentReference());
                orderRepository.save(savedOrder);
                orderCreatedCounter.increment();
                log.info("Order {} successfully paid and confirmed! Ref: {}", orderNumber, savedOrder.getPaymentReference());
            } else {
                String failureReason = (paymentResp != null && paymentResp.getMessage() != null)
                        ? paymentResp.getMessage()
                        : "Payment declined";
                handleOrderFailure(savedOrder, failureReason, compensatedProductIds, stockCompensations);
                throw new OrderProcessingException("Payment failed: " + failureReason);
            }
        } catch (OrderProcessingException ope) {
            throw ope;
        } catch (Exception ex) {
            log.error("Payment exception encountered for order: {}", orderNumber, ex);
            handleOrderFailure(savedOrder, ex.getMessage(), compensatedProductIds, stockCompensations);
            throw new OrderProcessingException("Payment processing failed: " + ex.getMessage(), ex);
        }

        return mapToResponse(savedOrder);
    }

    private void verifyCustomer(Long customerId) {
        try {
            ApiResponse<CustomerDto> customerResp = customerClient.getCustomerById(customerId);
            if (customerResp == null || customerResp.getData() == null) {
                throw new OrderProcessingException("Customer not found with id: " + customerId);
            }
            if ("CLOSED".equalsIgnoreCase(customerResp.getData().getStatus()) || "SUSPENDED".equalsIgnoreCase(customerResp.getData().getStatus())) {
                throw new OrderProcessingException("Customer account is " + customerResp.getData().getStatus());
            }
        } catch (OrderProcessingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to verify customer id: {}", customerId, e);
            throw new OrderProcessingException("Unable to verify customer: " + e.getMessage(), e);
        }
    }

    private void handleOrderFailure(Order order, String reason, List<Long> productIds, List<StockUpdateDto> compensations) {
        order.setStatus(OrderStatus.PAYMENT_FAILED);
        order.setFailureReason(reason);
        orderRepository.save(order);
        rollbackStock(productIds, compensations);
        orderFailedCounter.increment();
    }

    private void rollbackStock(List<Long> productIds, List<StockUpdateDto> compensations) {
        for (int i = 0; i < productIds.size(); i++) {
            try {
                Long productId = productIds.get(i);
                StockUpdateDto comp = compensations.get(i);
                productClient.restoreStock(productId, comp);
                log.info("Compensated/restored stock for product ID: {}", productId);
            } catch (Exception e) {
                log.error("Failed to restore stock for productId: {}", productIds.get(i), e);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        log.info("Fetching order by id: {}", id);
        return orderRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        log.info("Fetching order by orderNumber: {}", orderNumber);
        return orderRepository.findByOrderNumber(orderNumber)
                .map(this::mapToResponse)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with number: " + orderNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerId(Long customerId) {
        log.info("Fetching orders for customerId: {}", customerId);
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        log.info("Fetching all orders");
        return orderRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public OrderResponse cancelOrder(Long id) {
        log.info("Cancelling order with id: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new OrderProcessingException("Order is already cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        // Restore stock
        for (OrderItem item : order.getItems()) {
            try {
                StockUpdateDto restoreReq = StockUpdateDto.builder()
                        .quantity(item.getQuantity())
                        .reason("Order cancellation " + order.getOrderNumber())
                        .build();
                productClient.restoreStock(item.getProductId(), restoreReq);
            } catch (Exception ex) {
                log.error("Error restoring stock on order cancellation for product id {}", item.getProductId(), ex);
            }
        }

        Order updated = orderRepository.save(order);
        log.info("Order id: {} successfully cancelled", id);
        return mapToResponse(updated);
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productSku(item.getProductSku())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomerId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .paymentReference(order.getPaymentReference())
                .failureReason(order.getFailureReason())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
