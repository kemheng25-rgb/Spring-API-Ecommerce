package com.example.ecommerce.service;

import com.example.ecommerce.config.KafkaConfig;
import com.example.ecommerce.model.OutboxEvent;
import com.example.ecommerce.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxPublisherService")
class OutboxPublisherServiceTest {

    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxPublisherService outboxPublisherService;

    @BeforeEach
    void setUp() {
        outboxPublisherService = new OutboxPublisherService(outboxEventRepository, kafkaTemplate);
    }

    private OutboxEvent pendingEvent(long id, long aggregateId) {
        return OutboxEvent.builder()
            .id(id)
            .eventType("ORDER_PLACED")
            .aggregateId(aggregateId)
            .payload("{\"orderId\":" + aggregateId + "}")
            .status(OutboxEvent.Status.PENDING)
            .build();
    }

    @Test
    @DisplayName("marks a successfully published event PUBLISHED with a publishedAt timestamp")
    void publishesPendingEventSuccessfully() {
        OutboxEvent event = pendingEvent(1L, 100L);
        when(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEvent.Status.PENDING))
            .thenReturn(List.of(event));
        when(kafkaTemplate.send(eq(KafkaConfig.ORDER_EVENTS_TOPIC), eq("100"), eq(event.getPayload())))
            .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        outboxPublisherService.publishPendingEvents();

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OutboxEvent.Status.PUBLISHED);
        assertThat(captor.getValue().getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("marks the event FAILED (without throwing) if the Kafka send fails")
    void marksEventFailedOnSendError() {
        OutboxEvent event = pendingEvent(2L, 200L);
        when(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEvent.Status.PENDING))
            .thenReturn(List.of(event));
        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker unavailable"));
        when(kafkaTemplate.send(eq(KafkaConfig.ORDER_EVENTS_TOPIC), eq("200"), any())).thenReturn(failed);

        outboxPublisherService.publishPendingEvents();

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OutboxEvent.Status.FAILED);
    }

    @Test
    @DisplayName("does nothing when there are no pending events")
    void noPendingEventsIsNoOp() {
        when(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEvent.Status.PENDING))
            .thenReturn(List.of());

        outboxPublisherService.publishPendingEvents();

        verifyNoInteractions(kafkaTemplate);
        verify(outboxEventRepository, never()).save(any());
    }
}
