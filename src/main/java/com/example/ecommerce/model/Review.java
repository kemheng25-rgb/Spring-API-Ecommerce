package com.example.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews", indexes = {
    @Index(name = "idx_reviews_product_id", columnList = "product_id"),
    @Index(name = "idx_reviews_rating", columnList = "product_id, rating DESC"),
    @Index(name = "idx_reviews_created_at", columnList = "product_id, created_at DESC"),
    @Index(name = "idx_reviews_buyer_id", columnList = "buyer_id"),
    @Index(name = "idx_reviews_is_locked", columnList = "product_id, is_locked")
}, uniqueConstraints = {
    @UniqueConstraint(name = "unique_review_per_user_product", columnNames = {"product_id", "buyer_id"})
})
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"product", "buyer", "orderItem"})
public class Review {
    
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;  // Link to purchased item
    
    @Column(nullable = false)
    private Integer rating;  // 1-5 stars
    
    @Column(length = 200)
    private String reviewTitle;
    
    @Column(columnDefinition = "TEXT")
    private String reviewComment;
    
    @Builder.Default
    @Column(nullable = false)
    private Boolean verifiedPurchase = true;

    @Builder.Default
    @Column(nullable = false)
    private Integer helpfulCount = 0;

    @Column(columnDefinition = "TEXT")
    private String sellerResponse;

    @Column(name = "seller_response_at")
    private LocalDateTime sellerResponseAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus reviewStatus = ReviewStatus.PENDING;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isLocked = false;  // Locked after 30 days

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public enum ReviewStatus {
        PENDING, APPROVED, REJECTED, HIDDEN
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
