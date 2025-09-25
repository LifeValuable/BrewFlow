package ru.lifevaluable.brewflow.order.exception;

import java.util.UUID;

public class EmptyCartException extends RuntimeException {
    public EmptyCartException(UUID userId) {
        super(String.format("Can not create order for user %s with empty cart", userId.toString()));
    }
}
