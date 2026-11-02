package com.microservices.customer.exception;

import java.math.BigDecimal;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(BigDecimal currentBalance, BigDecimal requestedAmount) {
        super(String.format("Insufficient wallet balance: current balance is %s, requested deduction is %s",
                currentBalance, requestedAmount));
    }
}
