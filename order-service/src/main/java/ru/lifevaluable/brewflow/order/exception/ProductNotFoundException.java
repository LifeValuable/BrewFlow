package ru.lifevaluable.brewflow.order.exception;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(UUID productId) {
        super(String.format("Product is not found: %s", productId.toString()));
    }
}
