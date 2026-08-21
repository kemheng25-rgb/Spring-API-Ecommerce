package com.example.ecommerce.service;

import com.example.ecommerce.config.RabbitMQConfig;
import com.example.ecommerce.event.PaymentCompletedEvent;
import com.example.telegram.TelegramNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

import static com.example.telegram.TelegramNotifier.escapeHtml;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final TelegramNotifier telegramNotifier;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("[notification-service] sending payment confirmation for order {} (txn {})",
            event.orderNumber(), event.transactionId());

        telegramNotifier.sendMessage(buildInvoiceMessage(event));
    }

    private String buildInvoiceMessage(PaymentCompletedEvent event) {
        String itemLines = event.items().stream()
            .map(item -> "- %s x%d @ %s = %s".formatted(
                escapeHtml(item.productName()), item.quantity(), item.unitPrice(), item.subtotal()))
            .collect(Collectors.joining("\n"));

        return """
            <b>Payment Confirmed</b>
            Order: %s
            Buyer: %s
            Payment method: %s
            Transaction: %s

            <b>Items</b>
            %s

            <b>Total: %s</b>""".formatted(
            escapeHtml(event.orderNumber()), escapeHtml(event.buyerName()), escapeHtml(event.paymentMethod()),
            escapeHtml(event.transactionId()), itemLines, event.amount());
    }
}
