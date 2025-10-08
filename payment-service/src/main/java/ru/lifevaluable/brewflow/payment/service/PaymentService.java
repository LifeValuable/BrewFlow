package ru.lifevaluable.brewflow.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.lifevaluable.brewflow.payment.event.OrderCreatedEvent;
import ru.lifevaluable.brewflow.payment.event.PaymentProcessedEvent;
import ru.lifevaluable.brewflow.payment.model.PaymentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final EventPublisher eventPublisher;

    public void processPayment(OrderCreatedEvent orderEvent) {
        log.debug("Processing payment for order: userId={}, orderId={}, totalPrice={}", orderEvent.userId(), orderEvent.orderId(), orderEvent.totalPrice());
        log.debug("orderEvent: {}", orderEvent.toString());
        // mock: задержка выполнения платежа
        simulatePaymentDelay();

        // mock: 20% платежей неуспешные
        boolean isSuccess = Math.random() > 0.2;
        PaymentStatus status = isSuccess ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
        String errorMessage = isSuccess ? null : "Insufficient funds or card declined";


        UUID paymentId = UUID.randomUUID();

        PaymentProcessedEvent paymentEvent = new PaymentProcessedEvent(
                paymentId,
                orderEvent.orderId(),
                orderEvent.userId(),
                orderEvent.userEmail(),
                orderEvent.totalPrice(),
                status,
                errorMessage,
                LocalDateTime.now()
        );

        log.info("Payment processed: paymentId={}, userId={}, orderId={}, status={}", paymentId, orderEvent.userId(), orderEvent.orderId(), status);

        eventPublisher.publishPaymentProcessed(paymentEvent);
    }

    private void simulatePaymentDelay() {
        try {
            Thread.sleep(100 + (long)(Math.random() * 400));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
