package ru.lifevaluable.brewflow.order.dto;

import ru.lifevaluable.brewflow.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderHistoryResponse(
    UUID id,
    OrderStatus status,
    BigDecimal totalPrice,
    LocalDateTime createdAt,
    Integer itemsCount
) {}
