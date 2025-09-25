package ru.lifevaluable.brewflow.order.exception;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(UUID orderId) {
        super(String.format("Order not found: %s", orderId.toString()));
    }
}
