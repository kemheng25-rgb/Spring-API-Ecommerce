package com.example.ecommerce.service;

import com.example.ecommerce.config.KafkaConfig;
import com.example.ecommerce.event.OrderPlacedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventListener {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaConfig.ORDER_EVENTS_TOPIC, groupId = "inventory-service")
    public void onOrderPlaced(String payload) throws Exception {
        OrderPlacedEvent event = objectMapper.readValue(payload, OrderPlacedEvent.class);
        log.info("[inventory-service] reserving stock for order {} ({} line item(s))",
            event.orderNumber(), event.items().size());
    }
}
