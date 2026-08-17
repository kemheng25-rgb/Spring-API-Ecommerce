package com.example.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "carts", indexes = {
    @Index(name = "idx_carts_user_id", columnList = "user_id"),
    @Index(name = "idx_carts_expires_at", columnList = "expires_at")
}, uniqueConstraints = {
    @UniqueConstraint(name = "unique_active_cart_per_user", columnNames = "user_id")
})
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "items"})
public class Cart {
    
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime lastUpdatedAt = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);  // Expires after 30 days

    // Relationships
    @Builder.Default
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CartItem> items = new HashSet<>();

    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = LocalDateTime.now();
    }
}
