package com.microservices.payment.service;

import com.microservices.payment.dto.PaymentRequest;
import com.microservices.payment.dto.PaymentResponse;

import java.util.List;

public interface PaymentService {

    PaymentResponse processPayment(PaymentRequest request);

    PaymentResponse getPaymentById(Long id);

    PaymentResponse getPaymentByReference(String paymentReference);

    PaymentResponse getPaymentByOrderNumber(String orderNumber);

    List<PaymentResponse> getPaymentsByCustomerId(Long customerId);

    PaymentResponse refundPayment(Long id);
}
