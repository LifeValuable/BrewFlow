package ru.lifevaluable.brewflow.order.exception;

import ru.lifevaluable.brewflow.order.entity.OrderStatus;

import java.util.UUID;

public class InvalidOrderStatusTransitionException extends RuntimeException {
    public InvalidOrderStatusTransitionException(UUID orderId, OrderStatus currentStatus, OrderStatus newStatus) {
        super(String.format("Invalid status transition for order %s from %s to %s",
                orderId.toString(), currentStatus.toString(), newStatus.toString())
        );
    }
}
