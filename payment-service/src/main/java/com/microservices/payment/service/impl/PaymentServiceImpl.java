package com.microservices.payment.service.impl;

import com.microservices.payment.client.CustomerClient;
import com.microservices.payment.client.dto.WalletTransactionRequestDto;
import com.microservices.payment.dto.PaymentRequest;
import com.microservices.payment.dto.PaymentResponse;
import com.microservices.payment.entity.Payment;
import com.microservices.payment.entity.PaymentMethod;
import com.microservices.payment.entity.PaymentStatus;
import com.microservices.payment.exception.PaymentNotFoundException;
import com.microservices.payment.exception.PaymentProcessingException;
import com.microservices.payment.repository.PaymentRepository;
import com.microservices.payment.service.PaymentService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final CustomerClient customerClient;
    private final Counter paymentSuccessCounter;
    private final Counter paymentFailedCounter;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              CustomerClient customerClient,
                              MeterRegistry meterRegistry) {
        this.paymentRepository = paymentRepository;
        this.customerClient = customerClient;
        this.paymentSuccessCounter = Counter.builder("payments.success.count")
                .description("Number of successful payments")
                .register(meterRegistry);
        this.paymentFailedCounter = Counter.builder("payments.failed.count")
                .description("Number of failed payments")
                .register(meterRegistry);
    }

    @Override
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment for orderNumber: {}, amount: {}, method: {}",
                request.getOrderNumber(), request.getAmount(), request.getPaymentMethod());

        // Check idempotency: if order was already paid successfully, return existing payment
        Optional<Payment> existing = paymentRepository.findByOrderNumber(request.getOrderNumber());
        if (existing.isPresent() && existing.get().getStatus() == PaymentStatus.SUCCESS) {
            log.info("Idempotent check: Payment already processed for order {}", request.getOrderNumber());
            return mapToResponse(existing.get());
        }

        String paymentReference = "PAY-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();

        Payment payment = Payment.builder()
                .paymentReference(paymentReference)
                .orderNumber(request.getOrderNumber())
                .customerId(request.getCustomerId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .build();

        try {
            if (request.getPaymentMethod() == PaymentMethod.WALLET) {
                log.info("Deducting {} from customer wallet (customer id: {})", request.getAmount(), request.getCustomerId());
                WalletTransactionRequestDto walletReq = WalletTransactionRequestDto.builder()
                        .amount(request.getAmount())
                        .description("Payment for order " + request.getOrderNumber())
                        .build();
                customerClient.deductFromWallet(request.getCustomerId(), walletReq);
            } else {
                // Simulate card / UPI transaction processing
                log.info("Simulating external gateway authorization for {}", request.getPaymentMethod());
            }

            payment.setStatus(PaymentStatus.SUCCESS);
            Payment saved = paymentRepository.save(payment);
            paymentSuccessCounter.increment();
            log.info("Payment successful. Reference: {}", paymentReference);
            return mapToResponse(saved);

        } catch (Exception ex) {
            log.error("Payment failed for order: {}", request.getOrderNumber(), ex);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(ex.getMessage());
            paymentRepository.save(payment);
            paymentFailedCounter.increment();
            throw new PaymentProcessingException("Payment transaction failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id) {
        log.info("Fetching payment by id: {}", id);
        return paymentRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new PaymentNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByReference(String paymentReference) {
        log.info("Fetching payment by reference: {}", paymentReference);
        return paymentRepository.findByPaymentReference(paymentReference)
                .map(this::mapToResponse)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for reference: " + paymentReference));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderNumber(String orderNumber) {
        log.info("Fetching payment for orderNumber: {}", orderNumber);
        return paymentRepository.findByOrderNumber(orderNumber)
                .map(this::mapToResponse)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order number: " + orderNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByCustomerId(Long customerId) {
        log.info("Fetching payments for customerId: {}", customerId);
        return paymentRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PaymentResponse refundPayment(Long id) {
        log.info("Refunding payment with id: {}", id);
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new PaymentProcessingException("Cannot refund a payment with status: " + payment.getStatus());
        }

        if (payment.getPaymentMethod() == PaymentMethod.WALLET) {
            log.info("Refunding {} to customer wallet (customer id: {})", payment.getAmount(), payment.getCustomerId());
            WalletTransactionRequestDto walletReq = WalletTransactionRequestDto.builder()
                    .amount(payment.getAmount())
                    .description("Refund for order " + payment.getOrderNumber())
                    .build();
            customerClient.depositToWallet(payment.getCustomerId(), walletReq);
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        Payment updated = paymentRepository.save(payment);
        log.info("Payment reference {} refunded successfully", payment.getPaymentReference());
        return mapToResponse(updated);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .paymentReference(payment.getPaymentReference())
                .orderNumber(payment.getOrderNumber())
                .customerId(payment.getCustomerId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
