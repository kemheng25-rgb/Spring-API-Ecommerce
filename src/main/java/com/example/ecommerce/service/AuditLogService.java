package com.example.ecommerce.service;

import com.example.ecommerce.dto.AuditLogDTOs;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.model.AuditLog;
import com.example.ecommerce.model.User;
import com.example.ecommerce.repository.AuditLogRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Rule 8 (Role Separation): "Admin actions logged with timestamp and reason". This is the
 * generic append-only ledger other services call into - it never throws for a missing/null
 * userId (system actions are logged with no user, same as the schema's nullable user_id).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public void log(Long userId, String action, String entityType, Long entityId, Map<String, Object> changes) {
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;

        AuditLog entry = AuditLog.builder()
            .user(user)
            .action(action)
            .entityType(entityType)
            .entityId(entityId)
            .changes(changes)
            .timestamp(LocalDateTime.now())
            .build();
        auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDTOs.AuditLogResponse> getForUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDTOs.AuditLogResponse> getForEntity(String entityType, Long entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDTOs.AuditLogResponse> getByAction(String action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDTOs.AuditLogResponse> getInRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(start, end, pageable).map(this::mapToResponse);
    }

    private AuditLogDTOs.AuditLogResponse mapToResponse(AuditLog log) {
        return new AuditLogDTOs.AuditLogResponse(
            log.getId(),
            log.getUser() != null ? log.getUser().getId() : null,
            log.getUser() != null ? log.getUser().getFullName() : "system",
            log.getAction(),
            log.getEntityType(),
            log.getEntityId(),
            log.getChanges(),
            log.getIpAddress(),
            log.getTimestamp().toString()
        );
    }
}
