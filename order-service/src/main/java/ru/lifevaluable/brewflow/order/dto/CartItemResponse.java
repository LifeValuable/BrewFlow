package ru.lifevaluable.brewflow.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
        UUID productId,
        String productName,
        BigDecimal productPrice,
        Integer quantity,
        BigDecimal totalPrice
) {}
