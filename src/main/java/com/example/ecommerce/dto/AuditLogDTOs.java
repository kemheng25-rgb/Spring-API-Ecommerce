package com.example.ecommerce.dto;

import java.util.Map;

public class AuditLogDTOs {

    public record AuditLogResponse(
        Long id,
        Long userId,
        String userName,
        String action,
        String entityType,
        Long entityId,
        Map<String, Object> changes,
        String ipAddress,
        String timestamp
    ) {}
}
