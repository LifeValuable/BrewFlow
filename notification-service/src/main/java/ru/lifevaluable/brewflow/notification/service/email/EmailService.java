package ru.lifevaluable.brewflow.notification.service.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import ru.lifevaluable.brewflow.notification.event.OrderCreatedEvent;
import ru.lifevaluable.brewflow.notification.event.PaymentProcessedEvent;
import ru.lifevaluable.brewflow.notification.service.NotificationService;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService implements NotificationService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.properties.from}")
    private String fromEmail;

    @Override
    public void notify(OrderCreatedEvent orderEvent) {
        log.debug("Preparing to send OrderCreated email to: {} | orderId={}", orderEvent.userEmail(), orderEvent.orderId());

        String subject = String.format("Ваш заказ #%s создан", orderEvent.orderId());
        String body = buildOrderCreatedBody(orderEvent);

        sendEmail(orderEvent.userEmail(), subject, body);
    }

    @Override
    public void notify(PaymentProcessedEvent paymentEvent) {
        log.debug("Preparing to send PaymentProcessed email to: {} | paymentId={}", paymentEvent.userEmail(), paymentEvent.orderId());

        if (paymentEvent.status() == PaymentProcessedEvent.PaymentStatus.SUCCESS)
            notifyPaymentSuccess(paymentEvent);
        else
            notifyPaymentFailed(paymentEvent);
    }

    private void notifyPaymentSuccess(PaymentProcessedEvent paymentEvent) {
        String subject = String.format("Платёж по заказу #%s прошёл успешно", paymentEvent.orderId());
        String body = buildPaymentSuccessBody(paymentEvent);

        sendEmail(paymentEvent.userEmail(), subject, body);
    }

    private void notifyPaymentFailed(PaymentProcessedEvent paymentEvent) {
        String subject =  String.format("Ошибка оплаты заказа #%s", paymentEvent.orderId());
        String body = buildPaymentFailedBody(paymentEvent);

        sendEmail(paymentEvent.userEmail(), subject, body);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email to: {}. Error: {}", to, e.getMessage(), e);
            throw new RuntimeException("Email sending failed", e);
        }
    }

    private String buildOrderCreatedBody(OrderCreatedEvent orderEvent) {
        return String.format("""
            Здравствуйте, %s!
            
            Ваш заказ #%s на сумму %.2f руб. успешно создан.
            
            Детали заказа:
            %s
            
            Ожидайте обработку платежа.
            
            С уважением,
            Команда BrewFlow
            """,
                orderEvent.userFirstName(),
                orderEvent.orderId(),
                orderEvent.totalPrice(),
                formatOrderItems(orderEvent)
        );
    }

    private String buildPaymentSuccessBody(PaymentProcessedEvent paymentEvent) {
        return String.format("""
                Здравствуйте!
                
                Платёж по заказу #%s успешно обработан.
                Сумма: %.2f руб.
                
                Ваш заказ передан в доставку.
                
                С уважением,
                Команда BrewFlow
                """,
                paymentEvent.orderId(),
                paymentEvent.totalAmount()
        );
    }

    private String buildPaymentFailedBody(PaymentProcessedEvent paymentEvent) {
        return String.format("""
                Здравствуйте!
                
                К сожалению, не удалось обработать платёж по заказу #%s.
                
                Причина: %s
                
                Попробуйте оформить заказ снова или свяжитесь с поддержкой.
                
                С уважением,
                Команда BrewFlow
                """,
                paymentEvent.orderId(),
                paymentEvent.errorMessage() != null ? paymentEvent.errorMessage() : "Неизвестная ошибка"
        );
    }

    private String formatOrderItems(OrderCreatedEvent event) {
        StringBuilder items = new StringBuilder();
        for (var item : event.items()) {
            items.append(String.format("- %s x%d = %.2f руб.\n",
                    item.productName(),
                    item.quantity(),
                    item.priceAtTime()
            ));
        }
        return items.toString();
    }
}
