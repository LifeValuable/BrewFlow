package ru.lifevaluable.brewflow.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID productId,
        String productName,
        Integer quantity,
        BigDecimal priceAtTime,
        BigDecimal totalPrice
) {}
