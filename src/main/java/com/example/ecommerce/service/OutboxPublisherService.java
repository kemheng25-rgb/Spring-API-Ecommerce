package com.example.ecommerce.service;

import com.example.ecommerce.config.KafkaConfig;
import com.example.ecommerce.model.OutboxEvent;
import com.example.ecommerce.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Transactional outbox: OrderService writes an OutboxEvent row in the same DB transaction as the
 * order itself, so "order placed" and "event recorded" can never disagree, even if Kafka is down.
 * This poller is the only thing that talks to Kafka, decoupling "did we record the event" from
 * "is the broker reachable right now". Single-instance assumption - a second app instance would
 * re-poll and double-publish the same rows; a real deployment would add a claim column/lock or
 * switch to CDC (e.g. Debezium) instead of polling.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherService {

    /** After this many failed attempts an event is given up on and marked terminally FAILED. */
    static final int MAX_RETRIES = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEvent.Status.PENDING);
        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send(KafkaConfig.ORDER_EVENTS_TOPIC, event.getAggregateId().toString(), event.getPayload()).get();
                event.setStatus(OutboxEvent.Status.PUBLISHED);
                event.setPublishedAt(LocalDateTime.now());
            } catch (Exception ex) {
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= MAX_RETRIES) {
                    log.error("Outbox event {} ({}) exhausted {} retries, giving up", event.getId(), event.getEventType(), MAX_RETRIES, ex);
                    event.setStatus(OutboxEvent.Status.FAILED);
                } else {
                    log.warn("Failed to publish outbox event {} ({}), attempt {}/{} - will retry",
                        event.getId(), event.getEventType(), event.getRetryCount(), MAX_RETRIES, ex);
                    // Left PENDING: findTop50ByStatusOrderByCreatedAtAsc picks it up again next poll.
                }
            }
            outboxEventRepository.save(event);
        }
    }
}
