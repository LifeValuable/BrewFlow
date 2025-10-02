package ru.lifevaluable.brewflow.payment.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        UUID userId,
        String userEmail,
        String userFirstName,
        String userLastName,
        BigDecimal totalPrice,
        List<OrderItemEvent> items,
        LocalDateTime createdAt
) {
    public record OrderItemEvent(
            UUID productId,
            String productName,
            int quantity,
            BigDecimal priceAtTime
    ) {}
}
