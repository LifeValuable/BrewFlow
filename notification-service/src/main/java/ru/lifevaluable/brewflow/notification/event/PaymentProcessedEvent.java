package ru.lifevaluable.brewflow.notification.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentProcessedEvent(
        UUID paymentId,
        UUID orderId,
        UUID userId,
        String userEmail,
        BigDecimal totalAmount,
        PaymentStatus status,
        String errorMessage,  // null если успех
        LocalDateTime processedAt
) {
    public enum PaymentStatus {
        SUCCESS,
        FAILED
    }
}

