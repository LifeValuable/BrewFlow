package ru.lifevaluable.brewflow.order.dto;

import jakarta.validation.constraints.NotNull;
import ru.lifevaluable.brewflow.order.entity.OrderStatus;

import java.util.UUID;

public record UpdateOrderStatusRequest(
        @NotNull
        UUID userId,
        @NotNull
        OrderStatus currentStatus,
        @NotNull
        OrderStatus newStatus
) {}
