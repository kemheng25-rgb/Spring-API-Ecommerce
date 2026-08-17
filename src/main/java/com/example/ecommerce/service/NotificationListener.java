package com.example.ecommerce.service;

import com.example.ecommerce.config.RabbitMQConfig;
import com.example.ecommerce.event.PaymentCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationListener {

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("[notification-service] sending payment confirmation for order {} (txn {})",
            event.orderId(), event.transactionId());
    }
}
