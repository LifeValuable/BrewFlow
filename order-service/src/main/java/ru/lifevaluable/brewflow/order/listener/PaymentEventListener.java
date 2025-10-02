package ru.lifevaluable.brewflow.order.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ru.lifevaluable.brewflow.order.entity.OrderStatus;
import ru.lifevaluable.brewflow.order.event.PaymentProcessedEvent;
import ru.lifevaluable.brewflow.order.service.OrderService;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {
    private final OrderService orderService;

    @KafkaListener(topics = "payment-events", groupId = "order-service")
    public void handlePaymentProcessed(@Payload PaymentProcessedEvent paymentEvent,
                                       @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                       @Header(KafkaHeaders.OFFSET) long offset) {
        log.debug("Received PaymentProcessed event: orderId={}, userId={}, paymentId={} | partition={}, offset={}",
                paymentEvent.orderId(), paymentEvent.userId(), paymentEvent.paymentId(), partition, offset);
        if (paymentEvent.status().equals(PaymentProcessedEvent.PaymentStatus.SUCCESS)) {
            log.info("Payment status is success: orderId={}, userId={}, paymentId={}",
                    paymentEvent.orderId(), paymentEvent.userId(), paymentEvent.paymentId());
            orderService.updateOrderStatus(paymentEvent.orderId(), paymentEvent.userId(), OrderStatus.RESERVED, OrderStatus.PAYMENT_PROCESSED);
        } else {
            log.warn("Payment was failed: {} | orderId={}, userId={}, paymentId={}",
                    paymentEvent.errorMessage() == null ? "" : paymentEvent.errorMessage(), paymentEvent.orderId(),
                    paymentEvent.userId(), paymentEvent.paymentId());
            orderService.cancelOrder(paymentEvent.orderId(), paymentEvent.userId());
        }
    }
}
