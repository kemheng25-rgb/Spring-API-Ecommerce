package com.example.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One row per (idempotencyKey, scope) pair a client has submitted for a retry-sensitive
 * mutation (payment, order placement). See IdempotencyService for the claim/complete/release
 * lifecycle - this entity is just the storage.
 */
@Entity
@Table(name = "idempotency_keys", uniqueConstraints = {
    @UniqueConstraint(name = "uq_idempotency_key_scope", columnNames = {"idempotency_key", "scope"})
})
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKey {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    /** Discriminates which endpoint this key belongs to, e.g. "PAYMENT", "ORDER_PLACEMENT". */
    @Column(nullable = false, length = 30)
    private String scope;

    /** SHA-256 hex of the request's identifying fields - catches a key reused for a different request. */
    @Column(nullable = false, length = 64)
    private String requestFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(columnDefinition = "TEXT")
    private String responseBody;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime completedAt;

    public enum Status {
        IN_PROGRESS, COMPLETED
    }
}
