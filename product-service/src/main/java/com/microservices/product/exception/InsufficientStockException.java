package com.microservices.product.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String sku, int availableStock, int requestedQuantity) {
        super(String.format("Insufficient stock for SKU '%s': available=%d, requested=%d",
                sku, availableStock, requestedQuantity));
    }
}
