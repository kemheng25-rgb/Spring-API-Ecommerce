package com.example.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_items", indexes = {
    @Index(name = "idx_order_items_order_id", columnList = "order_id"),
    @Index(name = "idx_order_items_seller_id", columnList = "seller_id"),
    @Index(name = "idx_order_items_product_id", columnList = "product_id")
})
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"order", "product", "seller"})
public class OrderItem {
    
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private SellerProfile seller;  // Copied from product for efficiency
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;  // Price at order time
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;  // quantity * unitPrice
    
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemStatus itemStatus = ItemStatus.PENDING;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum ItemStatus {
        PENDING, CONFIRMED, PACKED, SHIPPED, DELIVERED, CANCELLED, RETURNED
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
