package com.example.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items", indexes = {
    @Index(name = "idx_cart_items_cart_id", columnList = "cart_id"),
    @Index(name = "idx_cart_items_product_id", columnList = "product_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "unique_product_per_cart", columnNames = {"cart_id", "product_id"})
})
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"cart", "product"})
public class CartItem {
    
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Builder.Default
    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtAdd;  // Lock price at cart add time

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime addedAt = LocalDateTime.now();
}
