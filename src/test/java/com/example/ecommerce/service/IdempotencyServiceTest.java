package com.example.ecommerce.service;

import com.example.ecommerce.exception.DuplicateResourceException;
import com.example.ecommerce.model.IdempotencyKey;
import com.example.ecommerce.repository.IdempotencyKeyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotencyService")
class IdempotencyServiceTest {

    @Mock private IdempotencyKeyRepository repository;

    private IdempotencyService idempotencyService;

    record Payload(String value) {}

    @BeforeEach
    void setUp() {
        idempotencyService = new IdempotencyService(repository, new ObjectMapper());
    }

    @Test
    @DisplayName("a fresh key claims successfully and saves an IN_PROGRESS row")
    void freshKeyClaims() {
        when(repository.findByIdempotencyKeyAndScope("key-1", "PAYMENT")).thenReturn(Optional.empty());

        Optional<Payload> result = idempotencyService.claim("key-1", "PAYMENT", "fp", Payload.class);

        assertThat(result).isEmpty();
        ArgumentCaptor<IdempotencyKey> captor = ArgumentCaptor.forClass(IdempotencyKey.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(IdempotencyKey.Status.IN_PROGRESS);
        assertThat(captor.getValue().getRequestFingerprint()).isEqualTo("fp");
    }

    @Test
    @DisplayName("a completed key replays the cached response instead of re-running the action")
    void completedKeyReplays() {
        IdempotencyKey record = IdempotencyKey.builder()
            .idempotencyKey("key-1").scope("PAYMENT").requestFingerprint("fp")
            .status(IdempotencyKey.Status.COMPLETED).responseBody("{\"value\":\"cached\"}")
            .build();
        when(repository.findByIdempotencyKeyAndScope("key-1", "PAYMENT")).thenReturn(Optional.of(record));

        Optional<Payload> result = idempotencyService.claim("key-1", "PAYMENT", "fp", Payload.class);

        assertThat(result).contains(new Payload("cached"));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("a key still IN_PROGRESS rejects a concurrent duplicate")
    void inProgressKeyRejectsDuplicate() {
        IdempotencyKey record = IdempotencyKey.builder()
            .idempotencyKey("key-1").scope("PAYMENT").requestFingerprint("fp")
            .status(IdempotencyKey.Status.IN_PROGRESS)
            .build();
        when(repository.findByIdempotencyKeyAndScope("key-1", "PAYMENT")).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> idempotencyService.claim("key-1", "PAYMENT", "fp", Payload.class))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("already being processed");
    }

    @Test
    @DisplayName("reusing a key with different request parameters is rejected")
    void fingerprintMismatchRejected() {
        IdempotencyKey record = IdempotencyKey.builder()
            .idempotencyKey("key-1").scope("PAYMENT").requestFingerprint("fp-original")
            .status(IdempotencyKey.Status.COMPLETED).responseBody("{}")
            .build();
        when(repository.findByIdempotencyKeyAndScope("key-1", "PAYMENT")).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> idempotencyService.claim("key-1", "PAYMENT", "fp-different", Payload.class))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("different request parameters");
    }

    @Test
    @DisplayName("losing the race on a concurrent insert is treated as an in-progress duplicate")
    void concurrentInsertRaceTreatedAsDuplicate() {
        when(repository.findByIdempotencyKeyAndScope("key-1", "PAYMENT")).thenReturn(Optional.empty());
        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("unique violation"));

        assertThatThrownBy(() -> idempotencyService.claim("key-1", "PAYMENT", "fp", Payload.class))
            .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("complete() stores the serialized response and marks the row COMPLETED")
    void completeStoresResponse() {
        IdempotencyKey record = IdempotencyKey.builder()
            .idempotencyKey("key-1").scope("PAYMENT").requestFingerprint("fp")
            .status(IdempotencyKey.Status.IN_PROGRESS)
            .build();
        when(repository.findByIdempotencyKeyAndScope("key-1", "PAYMENT")).thenReturn(Optional.of(record));

        idempotencyService.complete("key-1", "PAYMENT", new Payload("done"));

        ArgumentCaptor<IdempotencyKey> captor = ArgumentCaptor.forClass(IdempotencyKey.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(IdempotencyKey.Status.COMPLETED);
        assertThat(captor.getValue().getResponseBody()).contains("done");
    }

    @Test
    @DisplayName("release() deletes the row so the same key can be retried")
    void releaseDeletesRow() {
        idempotencyService.release("key-1", "PAYMENT");

        verify(repository).deleteByIdempotencyKeyAndScope("key-1", "PAYMENT");
    }

    @Test
    @DisplayName("fingerprint() is deterministic for the same inputs and differs for different ones")
    void fingerprintIsDeterministic() {
        String a = IdempotencyService.fingerprint(1L, "CREDIT_CARD", "10.00");
        String b = IdempotencyService.fingerprint(1L, "CREDIT_CARD", "10.00");
        String c = IdempotencyService.fingerprint(1L, "CREDIT_CARD", "20.00");

        assertThat(a).isEqualTo(b);
        assertThat(a).isNotEqualTo(c);
    }
}
