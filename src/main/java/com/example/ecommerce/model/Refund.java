package com.example.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refunds", indexes = {
    @Index(name = "idx_refunds_payment_id", columnList = "payment_id"),
    @Index(name = "idx_refunds_refund_status", columnList = "refund_status"),
    @Index(name = "idx_refunds_refund_date", columnList = "refund_date DESC")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_refund_transaction_id", columnNames = "refund_transaction_id")
})
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"payment", "initiatedByUser"})
public class Refund {
    
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal refundAmount;
    
    @Column(length = 100)
    private String refundReason;  // RETURN, CANCELLATION, DISPUTE
    
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus refundStatus = RefundStatus.INITIATED;
    
    @Column(unique = true, length = 255)
    private String refundTransactionId;  // From payment gateway
    
    @Column(name = "refund_date")
    private LocalDateTime refundDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiated_by_user_id")
    private User initiatedByUser;  // Who initiated the refund
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum RefundStatus {
        INITIATED, PENDING, COMPLETED, FAILED, DISPUTED
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
