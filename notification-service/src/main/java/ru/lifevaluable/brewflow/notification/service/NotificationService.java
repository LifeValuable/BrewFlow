package ru.lifevaluable.brewflow.notification.service;

import ru.lifevaluable.brewflow.notification.event.OrderCreatedEvent;
import ru.lifevaluable.brewflow.notification.event.PaymentProcessedEvent;

public interface NotificationService {
    void notify(OrderCreatedEvent orderEvent);
    void notify(PaymentProcessedEvent paymentEvent);
}
