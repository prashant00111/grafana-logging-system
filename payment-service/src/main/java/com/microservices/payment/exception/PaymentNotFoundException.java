package com.microservices.payment.exception;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String message) {
        super(message);
    }

    public PaymentNotFoundException(Long id) {
        super("Payment not found with id: " + id);
    }
}
