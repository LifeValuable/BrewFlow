package ru.lifevaluable.brewflow.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import ru.lifevaluable.brewflow.payment.event.PaymentProcessedEvent;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPaymentProcessed(PaymentProcessedEvent event) {
        String topicName = "payment-events";
        String key = event.orderId().toString();
        log.debug("Publishing PaymentProcessed event: paymentId={}, userId={}, orderId={}, status={}",
                event.paymentId(), event.userId(), event.orderId(), event.status());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topicName, key, event);
        future.whenComplete((result, exception) -> {
            if (exception == null) {
                log.info("PaymentProcessed event published: paymentId={}, offset={}",
                        event.paymentId(), result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish PaymentProcessed event: paymentId={}", event.paymentId(), exception);
            }

        });
    }
}
