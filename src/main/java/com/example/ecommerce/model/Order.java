package com.example.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_buyer_id", columnList = "buyer_id"),
    @Index(name = "idx_orders_seller_id", columnList = "seller_id"),
    @Index(name = "idx_orders_order_status", columnList = "order_status"),
    @Index(name = "idx_orders_order_date", columnList = "order_date DESC"),
    @Index(name = "idx_orders_buyer_date", columnList = "buyer_id, order_date DESC"),
    @Index(name = "idx_orders_tracking_number", columnList = "tracking_number")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_order_number", columnNames = "order_number")
})
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"buyer", "seller", "shippingAddress", "billingAddress", "items", "payments", "dispute"})
public class Order {
    
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String orderNumber;  // ORD-2026-001234
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private SellerProfile seller;  // Can be NULL for multi-seller orders
    
    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime orderDate = LocalDateTime.now();

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus = OrderStatus.PENDING;
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_address_id", nullable = false)
    private Address shippingAddress;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_address_id")
    private Address billingAddress;
    
    @Column(length = 50)
    private String shippingMethod;  // STANDARD, EXPRESS, OVERNIGHT
    
    @Column(length = 100)
    private String trackingNumber;
    
    @Column(name = "estimated_delivery_date")
    private LocalDate estimatedDeliveryDate;
    
    @Column(name = "actual_delivery_date")
    private LocalDate actualDeliveryDate;
    
    @Column(name = "return_initiated_at")
    private LocalDateTime returnInitiatedAt;
    
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
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OrderItem> items = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Payment> payments = new HashSet<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Dispute dispute;

    public enum OrderStatus {
        PENDING, CONFIRMED, PACKED, SHIPPED, DELIVERED, CANCELLED, RETURNED
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
