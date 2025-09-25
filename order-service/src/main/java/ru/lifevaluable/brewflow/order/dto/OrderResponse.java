package ru.lifevaluable.brewflow.order.dto;

import ru.lifevaluable.brewflow.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

public record OrderResponse(
        UUID id,
        UUID userId,
        String userFirstName,
        String userLastName,
        String userEmail,
        List<OrderItemResponse> items,
        OrderStatus status,
        BigDecimal totalPrice,
        LocalDateTime createdAt
) {}
