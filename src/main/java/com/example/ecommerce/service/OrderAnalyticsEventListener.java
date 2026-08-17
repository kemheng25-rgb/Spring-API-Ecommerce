package com.example.ecommerce.service;

import com.example.ecommerce.config.KafkaConfig;
import com.example.ecommerce.event.OrderPlacedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Independent consumer group reading the same order.events topic as InventoryEventListener -
 * demonstrates Kafka's pub/sub fan-out: every consumer group gets its own full copy of the
 * stream, unlike a RabbitMQ queue where competing consumers split messages between them.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderAnalyticsEventListener {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaConfig.ORDER_EVENTS_TOPIC, groupId = "analytics-service")
    public void onOrderPlaced(String payload) throws Exception {
        OrderPlacedEvent event = objectMapper.readValue(payload, OrderPlacedEvent.class);
        log.info("[analytics-service] recorded order {} totalling {}", event.orderNumber(), event.totalAmount());
    }
}
