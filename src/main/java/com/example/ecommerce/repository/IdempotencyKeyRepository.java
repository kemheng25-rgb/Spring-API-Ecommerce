package com.example.ecommerce.repository;

import com.example.ecommerce.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {
    Optional<IdempotencyKey> findByIdempotencyKeyAndScope(String idempotencyKey, String scope);
    void deleteByIdempotencyKeyAndScope(String idempotencyKey, String scope);
}
