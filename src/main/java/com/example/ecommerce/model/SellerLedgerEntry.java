package com.example.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Append-only audit trail of a seller's earnings. A row is never updated after insert -
 * a refund or a post-payment cancellation is recorded as a new reversing row
 * (REFUND_ADJUSTMENT / CANCELLATION), never as a mutation of the original SALE row, so the
 * full history of what happened to an order item's earnings stays visible. See
 * SellerLedgerService for where each entry type gets written.
 */
@Entity
@Table(name = "seller_ledger_entries", indexes = {
    @Index(name = "idx_seller_ledger_entries_seller_id", columnList = "seller_id"),
    @Index(name = "idx_seller_ledger_entries_order_item_id", columnList = "order_item_id"),
    @Index(name = "idx_seller_ledger_entries_created_at", columnList = "created_at")
})
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"seller", "orderItem"})
public class SellerLedgerEntry {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private SellerProfile seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EntryType entryType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal grossAmount;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionRate;  // Snapshot of SellerProfile.commissionRate at the time this entry was written

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal commissionAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal netAmount;

    @Column(length = 255)
    private String description;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum EntryType {
        SALE, REFUND_ADJUSTMENT, CANCELLATION
    }
}
