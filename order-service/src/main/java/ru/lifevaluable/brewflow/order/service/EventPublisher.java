package ru.lifevaluable.brewflow.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import ru.lifevaluable.brewflow.order.event.OrderCreatedEvent;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        String topicName = "order-events";
        String key = event.orderId().toString();
        log.debug("Publishing OrderCreated event: orderId={}, userId={}", event.orderId(), event.userId());
        log.debug("orderCreated={}", event.toString());
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topicName, key, event);
        future.whenComplete((result, exception) -> {
            if (exception == null) {
                log.info("OrderCreated event published successfully: orderId={}, offset={}", event.orderId(), result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish OrderCreated event: orderId={}", event.orderId(), exception);
            }

        });
    }
}
