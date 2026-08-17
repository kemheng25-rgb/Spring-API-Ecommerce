package com.example.ecommerce.service;

import com.example.ecommerce.config.RabbitMQConfig;
import com.example.ecommerce.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * RabbitMQ's answer to the same dual-write problem the outbox solves for Kafka, handled more
 * lightly: an in-process Spring event fired inside the payment transaction, delivered to the
 * broker only AFTER_COMMIT so a rolled-back payment never triggers a notification. There's no
 * durable outbox row behind it - a crash between commit and publish loses the message - which is
 * an acceptable trade for a best-effort side effect like an email, but not for the order event
 * fan-out Kafka handles.
 */
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATION_QUEUE, event);
    }
}
