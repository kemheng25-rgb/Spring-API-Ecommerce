package com.example.ecommerce.service;

import com.example.ecommerce.event.PaymentCompletedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("NotificationListener")
class NotificationListenerTest {

    private final NotificationListener listener = new NotificationListener();

    @Test
    @DisplayName("handles a PaymentCompletedEvent without throwing")
    void handlesPaymentCompletedEvent() {
        PaymentCompletedEvent event = new PaymentCompletedEvent(1L, 10L, 1L, new BigDecimal("50.00"), "TXN-abc");

        assertThatCode(() -> listener.onPaymentCompleted(event)).doesNotThrowAnyException();
    }
}
