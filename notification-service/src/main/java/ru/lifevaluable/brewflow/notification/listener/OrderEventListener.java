package ru.lifevaluable.brewflow.notification.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ru.lifevaluable.brewflow.notification.event.OrderCreatedEvent;
import ru.lifevaluable.brewflow.notification.service.NotificationService;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {
    private final NotificationService notificationService;

    @KafkaListener(topics = "order-events", groupId = "notification-service")
    public void handleOrderCreated(@Payload OrderCreatedEvent orderEvent,
                                   @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                   @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received OrderCreated event: orderId={}, userId={}, totalAmount={} | partition={}, offset={}",
                orderEvent.orderId(), orderEvent.userId(), orderEvent.totalPrice(), partition, offset);

        notificationService.notify(orderEvent);
    }
}
