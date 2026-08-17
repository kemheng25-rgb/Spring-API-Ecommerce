package com.example.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payments_order_id", columnList = "order_id"),
    @Index(name = "idx_payments_buyer_id", columnList = "buyer_id"),
    @Index(name = "idx_payments_payment_status", columnList = "payment_status"),
    @Index(name = "idx_payments_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_payments_payment_date", columnList = "payment_date DESC")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_transaction_id", columnNames = "transaction_id")
})
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"order", "buyer", "refunds"})
public class Payment {
    
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    
    @Builder.Default
    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    
    @Column(unique = true, length = 255)
    private String transactionId;  // From payment gateway
    
    @Column(length = 50)
    private String paymentGateway;  // STRIPE, PAYPAL, etc.
    
    @Column(name = "payment_date")
    private LocalDateTime paymentDate;
    
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus refundStatus = RefundStatus.NOT_REFUNDED;

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalRefundedAmount = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Relationships
    @Builder.Default
    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Refund> refunds = new HashSet<>();
    
    public enum PaymentMethod {
        CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, WALLET, OTHER
    }
    
    public enum PaymentStatus {
        PENDING, COMPLETED, FAILED, REFUNDED
    }
    
    public enum RefundStatus {
        NOT_REFUNDED, PARTIALLY_REFUNDED, FULLY_REFUNDED
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
