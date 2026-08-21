package com.example.ecommerce.service;

import com.example.ecommerce.config.RabbitMQConfig;
import com.example.ecommerce.event.PaymentCompletedEvent;
import com.example.telegram.TelegramNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final TelegramNotifier telegramNotifier;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("[notification-service] sending payment confirmation for order {} (txn {})",
            event.orderId(), event.transactionId());

        telegramNotifier.sendMessage(
            "Payment confirmed for order #%d - amount %s (txn %s)".formatted(
                event.orderId(), event.amount(), event.transactionId()));
    }
}
