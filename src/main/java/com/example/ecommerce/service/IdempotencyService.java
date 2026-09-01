package com.example.ecommerce.service;

import com.example.ecommerce.exception.DuplicateResourceException;
import com.example.ecommerce.model.IdempotencyKey;
import com.example.ecommerce.repository.IdempotencyKeyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Client-supplied Idempotency-Key support for retry-sensitive mutations (payment, order
 * placement) - see PaymentService/OrderService for how each wraps its business logic with
 * claim/complete/release. Each of those three runs in its own REQUIRES_NEW transaction,
 * deliberately independent of the caller's own @Transactional method: the claim must be
 * durably visible to a concurrent duplicate request the instant it's made (not just after the
 * caller's whole transaction commits), and release must survive even when the caller's
 * transaction rolls back on failure - a self-invocation via normal propagation would get
 * silently absorbed into the caller's transaction and defeat both of those guarantees.
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;

    /** Deterministic fingerprint of a request's identifying fields, to catch a key reused for a different request. */
    public static String fingerprint(Object... parts) {
        StringBuilder joined = new StringBuilder();
        for (Object part : parts) {
            joined.append(part).append('|');
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(joined.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available on the JVM", e);
        }
    }

    /**
     * Claims a key for a fresh attempt, or returns the cached response for a genuine replay.
     * Empty return means: proceed with the business logic, then call complete() or release().
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> Optional<T> claim(String key, String scope, String fingerprint, Class<T> responseType) {
        Optional<IdempotencyKey> existing = repository.findByIdempotencyKeyAndScope(key, scope);
        if (existing.isPresent()) {
            IdempotencyKey record = existing.get();
            if (!record.getRequestFingerprint().equals(fingerprint)) {
                throw new DuplicateResourceException(
                    "Idempotency key '" + key + "' was already used with different request parameters");
            }
            if (record.getStatus() == IdempotencyKey.Status.COMPLETED) {
                return Optional.of(readJson(record.getResponseBody(), responseType));
            }
            throw new DuplicateResourceException(
                "A request with idempotency key '" + key + "' is already being processed");
        }

        try {
            repository.save(IdempotencyKey.builder()
                .idempotencyKey(key)
                .scope(scope)
                .requestFingerprint(fingerprint)
                .status(IdempotencyKey.Status.IN_PROGRESS)
                .build());
        } catch (DataIntegrityViolationException e) {
            // Lost the race to a concurrent request carrying the same key.
            throw new DuplicateResourceException(
                "A request with idempotency key '" + key + "' is already being processed");
        }
        return Optional.empty();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(String key, String scope, Object response) {
        repository.findByIdempotencyKeyAndScope(key, scope).ifPresent(record -> {
            record.setStatus(IdempotencyKey.Status.COMPLETED);
            record.setResponseBody(writeJson(response));
            record.setCompletedAt(LocalDateTime.now());
            repository.save(record);
        });
    }

    /** Lets a genuinely failed attempt be retried under the same key, instead of being stuck IN_PROGRESS forever. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(String key, String scope) {
        repository.deleteByIdempotencyKeyAndScope(key, scope);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize idempotent response", e);
        }
    }

    private <T> T readJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize cached idempotent response", e);
        }
    }
}
