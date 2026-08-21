package com.example.ecommerce.service;

import com.example.ecommerce.config.RabbitMQConfig;
import com.example.ecommerce.event.PaymentCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationPublisher")
class NotificationPublisherTest {

    @Mock private RabbitTemplate rabbitTemplate;

    private NotificationPublisher notificationPublisher;

    @BeforeEach
    void setUp() {
        notificationPublisher = new NotificationPublisher(rabbitTemplate);
    }

    @Test
    @DisplayName("forwards a PaymentCompletedEvent to the notification queue after commit")
    void onPaymentCompletedSendsToRabbit() {
        PaymentCompletedEvent event = new PaymentCompletedEvent(
            1L, 10L, "ORD-2026-000010", 1L, "Buyer 1", new BigDecimal("50.00"), "TXN-abc");

        notificationPublisher.onPaymentCompleted(event);

        verify(rabbitTemplate).convertAndSend(RabbitMQConfig.NOTIFICATION_QUEUE, event);
    }
}
