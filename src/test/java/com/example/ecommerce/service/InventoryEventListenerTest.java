package com.example.ecommerce.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InventoryEventListener")
class InventoryEventListenerTest {

    private final InventoryEventListener listener = new InventoryEventListener(new ObjectMapper());

    @Test
    @DisplayName("parses a well-formed OrderPlacedEvent payload without throwing")
    void handlesValidPayload() {
        String payload = "{\"orderId\":1,\"orderNumber\":\"ORD-2026-000001\",\"buyerId\":1,"
            + "\"totalAmount\":39.98,\"items\":[{\"productId\":100,\"productName\":\"Widget\",\"quantity\":2}]}";

        assertThatCode(() -> listener.onOrderPlaced(payload)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("propagates a parse failure for malformed JSON rather than silently dropping it")
    void rejectsMalformedPayload() {
        assertThatThrownBy(() -> listener.onOrderPlaced("not-json")).isInstanceOf(Exception.class);
    }
}
