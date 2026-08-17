package com.example.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_logs_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_audit_logs_user_id", columnList = "user_id"),
    @Index(name = "idx_audit_logs_action", columnList = "action"),
    @Index(name = "idx_audit_logs_timestamp", columnList = "timestamp DESC"),
    @Index(name = "idx_audit_logs_entity_time", columnList = "entity_type, entity_id, timestamp DESC")
})
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "user")
public class AuditLog {
    
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;  // Who performed the action (NULL for system actions)
    
    @Column(nullable = false, length = 50)
    private String action;  // LOGIN, CREATE_PRODUCT, UPDATE_ORDER, REFUND, etc.
    
    @Column(nullable = false, length = 50)
    private String entityType;  // ORDER, PAYMENT, PRODUCT, USER, etc.
    
    @Column(nullable = false)
    private Long entityId;  // ID of the affected record
    
    @Type(JsonType.class)
    @Column(columnDefinition = "json")
    private java.util.Map<String, Object> changes;  // Stores before/after values
    
    @Column(length = 45)
    private String ipAddress;  // IPv4 or IPv6
    
    @Column(length = 500)
    private String userAgent;  // Browser/device info
    
    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
}
