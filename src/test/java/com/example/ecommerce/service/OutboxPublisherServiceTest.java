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
    @DisplayName("leaves a failed event PENDING with an incremented retryCount when retries remain")
    void retriesOnSendErrorBelowMaxRetries() {
        OutboxEvent event = pendingEvent(2L, 200L);
        when(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEvent.Status.PENDING))
            .thenReturn(List.of(event));
        when(kafkaTemplate.send(eq(KafkaConfig.ORDER_EVENTS_TOPIC), eq("200"), any())).thenReturn(failedSend());

        outboxPublisherService.publishPendingEvents();

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OutboxEvent.Status.PENDING);
        assertThat(captor.getValue().getRetryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("gives up and marks the event FAILED once retryCount reaches MAX_RETRIES")
    void marksEventFailedOnceRetriesExhausted() {
        OutboxEvent event = pendingEvent(3L, 300L);
        event.setRetryCount(OutboxPublisherService.MAX_RETRIES - 1);
        when(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEvent.Status.PENDING))
            .thenReturn(List.of(event));
        when(kafkaTemplate.send(eq(KafkaConfig.ORDER_EVENTS_TOPIC), eq("300"), any())).thenReturn(failedSend());

        outboxPublisherService.publishPendingEvents();

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OutboxEvent.Status.FAILED);
        assertThat(captor.getValue().getRetryCount()).isEqualTo(OutboxPublisherService.MAX_RETRIES);
    }

    private CompletableFuture<SendResult<String, String>> failedSend() {
        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker unavailable"));
        return failed;
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
