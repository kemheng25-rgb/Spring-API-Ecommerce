package com.example.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "disputes", indexes = {
    @Index(name = "idx_disputes_order_id", columnList = "order_id"),
    @Index(name = "idx_disputes_buyer_id", columnList = "buyer_id"),
    @Index(name = "idx_disputes_seller_id", columnList = "seller_id"),
    @Index(name = "idx_disputes_status", columnList = "dispute_status"),
    @Index(name = "idx_disputes_created_at", columnList = "created_at DESC"),
    @Index(name = "idx_disputes_assigned_admin_id", columnList = "assigned_admin_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_dispute_number", columnNames = "dispute_number")
})
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"order", "buyer", "seller", "assignedAdmin"})
public class Dispute {
    
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String disputeNumber;  // DSP-2026-001
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private SellerProfile seller;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InitiatedBy initiatedBy;
    
    @Column(nullable = false, length = 100)
    private String disputeReason;  // NOT_RECEIVED, DAMAGED, WRONG_ITEM, QUALITY_ISSUE
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String evidenceUrls;  // JSON array
    
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisputeStatus disputeStatus = DisputeStatus.OPEN;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Resolution resolution = Resolution.NONE;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_admin_id")
    private User assignedAdmin;
    
    @Column(columnDefinition = "TEXT")
    private String adminNotes;
    
    @Column(columnDefinition = "TEXT")
    private String resolutionNotes;
    
    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    public enum InitiatedBy {
        BUYER, SELLER
    }
    
    public enum DisputeStatus {
        OPEN, IN_REVIEW, RESOLVED, CLOSED, APPEALED
    }
    
    public enum Resolution {
        REFUND, REPLACEMENT, CREDIT, NONE
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
