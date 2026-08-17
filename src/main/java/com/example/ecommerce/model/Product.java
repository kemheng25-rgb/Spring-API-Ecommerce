package com.example.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_products_seller_id", columnList = "seller_id"),
    @Index(name = "idx_products_category_id", columnList = "category_id"),
    @Index(name = "idx_products_product_status", columnList = "product_status"),
    @Index(name = "idx_products_sku", columnList = "sku"),
    @Index(name = "idx_products_category_status", columnList = "category_id, product_status"),
    @Index(name = "idx_products_rating_status", columnList = "average_rating DESC, product_status")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_sku", columnNames = "sku")
})
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"seller", "category", "images", "cartItems", "orderItems", "reviews"})
public class Product {
    
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private SellerProfile seller;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    
    @Column(nullable = false, length = 255)
    private String productName;
    
    @Column(columnDefinition = "TEXT")
    private String productDescription;
    
    @Column(nullable = false, unique = true, length = 100)
    private String sku;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Builder.Default
    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Builder.Default
    @Column(nullable = false)
    private Integer stockQuantity = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus productStatus = ProductStatus.ACTIVE;

    @Builder.Default
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercentage = BigDecimal.ZERO;  // 0.00 to 100.00

    @Column(name = "discount_valid_until")
    private LocalDateTime discountValidUntil;

    @Builder.Default
    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO;  // Denormalized

    @Builder.Default
    @Column(nullable = false)
    private Integer totalReviews = 0;  // Denormalized

    @Builder.Default
    @Column(nullable = false)
    private Integer viewsCount = 0;  // Analytics

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Relationships
    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductImage> images = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CartItem> cartItems = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OrderItem> orderItems = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Review> reviews = new HashSet<>();

    public enum ProductStatus {
        ACTIVE, INACTIVE, DELISTED
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
