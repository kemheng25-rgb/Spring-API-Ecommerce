package com.example.ecommerce.service;

import com.example.ecommerce.event.PaymentCompletedEvent;
import com.example.telegram.TelegramNotifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

@DisplayName("NotificationListener")
class NotificationListenerTest {

    private final NotificationListener listener = new NotificationListener(mock(TelegramNotifier.class));

    @Test
    @DisplayName("handles a PaymentCompletedEvent without throwing")
    void handlesPaymentCompletedEvent() {
        PaymentCompletedEvent event = new PaymentCompletedEvent(
            1L, 10L, "ORD-2026-000010", 1L, "Buyer 1", new BigDecimal("50.00"), "TXN-abc");

        assertThatCode(() -> listener.onPaymentCompleted(event)).doesNotThrowAnyException();
    }
}
