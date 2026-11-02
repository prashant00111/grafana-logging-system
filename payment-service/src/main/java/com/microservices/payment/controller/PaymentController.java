package com.microservices.payment.controller;

import com.microservices.payment.dto.ApiResponse;
import com.microservices.payment.dto.PaymentRequest;
import com.microservices.payment.dto.PaymentResponse;
import com.microservices.payment.service.PaymentService;
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
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Management", description = "Endpoints for processing and querying payment transactions")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    @Operation(summary = "Process a payment for an order")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(@Valid @RequestBody PaymentRequest request) {
        log.info("REST request to process payment for orderNumber: {}", request.getOrderNumber());
        PaymentResponse response = paymentService.processPayment(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location)
                .body(ApiResponse.success(response, "Payment processed successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(@PathVariable Long id) {
        log.info("REST request to get payment by ID: {}", id);
        PaymentResponse response = paymentService.getPaymentById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/reference/{paymentReference}")
    @Operation(summary = "Get payment by reference")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByReference(@PathVariable String paymentReference) {
        log.info("REST request to get payment by reference: {}", paymentReference);
        PaymentResponse response = paymentService.getPaymentByReference(paymentReference);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/order/{orderNumber}")
    @Operation(summary = "Get payment by order number")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrderNumber(@PathVariable String orderNumber) {
        log.info("REST request to get payment by order number: {}", orderNumber);
        PaymentResponse response = paymentService.getPaymentByOrderNumber(orderNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get payments by customer ID")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByCustomerId(@PathVariable Long customerId) {
        log.info("REST request to get payments for customer ID: {}", customerId);
        List<PaymentResponse> responses = paymentService.getPaymentsByCustomerId(customerId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Refund a payment transaction")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(@PathVariable Long id) {
        log.info("REST request to refund payment ID: {}", id);
        PaymentResponse response = paymentService.refundPayment(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment refunded successfully"));
    }
}
