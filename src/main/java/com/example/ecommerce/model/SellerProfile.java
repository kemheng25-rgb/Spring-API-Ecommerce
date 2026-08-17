package com.example.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "seller_profiles", indexes = {
    @Index(name = "idx_seller_profiles_verification_status", columnList = "verification_status"),
    @Index(name = "idx_seller_profiles_seller_rating", columnList = "seller_rating"),
    @Index(name = "idx_seller_profiles_shop_name", columnList = "shop_name")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_seller_user_id", columnNames = "user_id"),
    @UniqueConstraint(name = "uk_shop_name", columnNames = "shop_name")
})
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "products", "orders"})
public class SellerProfile {
    
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, unique = true, length = 100)
    private String shopName;
    
    @Column(columnDefinition = "TEXT")
    private String shopDescription;
    
    @Column(length = 500)
    private String shopLogoUrl;
    
    @Builder.Default
    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal sellerRating = BigDecimal.ZERO;  // 0.00 to 5.00

    @Builder.Default
    @Column(nullable = false)
    private Integer totalProductsSold = 0;

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

    @Column(length = 500)
    private String bankAccountEncrypted;  // Stored encrypted

    @Builder.Default
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionRate = new BigDecimal("10.00");  // Percentage

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime shopCreatedAt = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Relationships
    @Builder.Default
    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Product> products = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Order> orders = new HashSet<>();

    public enum VerificationStatus {
        UNVERIFIED, VERIFIED, REJECTED
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
