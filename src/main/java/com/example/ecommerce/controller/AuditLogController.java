package com.example.ecommerce.controller;

import com.example.ecommerce.dto.AuditLogDTOs;
import com.example.ecommerce.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Log", description = "Admin: read-only compliance/fraud-investigation trail")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping(params = "userId")
    @Operation(summary = "All actions performed by (or on) a given user")
    public ResponseEntity<Page<AuditLogDTOs.AuditLogResponse>> byUser(@RequestParam Long userId, Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getForUser(userId, pageable));
    }

    @GetMapping(params = {"entityType", "entityId"})
    @Operation(summary = "Full change history for one record")
    public ResponseEntity<Page<AuditLogDTOs.AuditLogResponse>> byEntity(
            @RequestParam String entityType, @RequestParam Long entityId, Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getForEntity(entityType, entityId, pageable));
    }

    @GetMapping(params = "action")
    @Operation(summary = "All occurrences of one action type")
    public ResponseEntity<Page<AuditLogDTOs.AuditLogResponse>> byAction(@RequestParam String action, Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getByAction(action, pageable));
    }

    @GetMapping(params = {"start", "end"})
    @Operation(summary = "All log entries in a time range")
    public ResponseEntity<Page<AuditLogDTOs.AuditLogResponse>> inRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getInRange(start, end, pageable));
    }
}
