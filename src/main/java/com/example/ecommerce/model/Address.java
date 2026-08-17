package com.example.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "addresses", indexes = {
    @Index(name = "idx_addresses_user_id", columnList = "user_id"),
    @Index(name = "idx_addresses_is_default", columnList = "user_id, is_default")
})
// NOTE: a composite UNIQUE(user_id, is_default) used to live here to enforce "one default
// address per user" - it doesn't work: with is_default defaulting to false, a user's SECOND
// non-default address collides on (user_id, false). Postgres fixes this with a partial unique
// index (`WHERE is_default = true`); JPA/Hibernate can't express that, so "only one default"
// is enforced in AddressService instead (unset the old default before setting a new one).
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "user")
public class Address {
    
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, length = 100)
    private String fullName;
    
    @Column(nullable = false, length = 20)
    private String phone;
    
    @Column(nullable = false, length = 255)
    private String streetAddress;
    
    @Column(nullable = false, length = 100)
    private String city;
    
    @Column(nullable = false, length = 100)
    private String stateProvince;
    
    @Column(nullable = false, length = 20)
    private String postalCode;
    
    @Column(nullable = false, length = 100)
    private String country;
    
    @Builder.Default
    @Column(nullable = false)
    private Boolean isDefault = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AddressType addressType = AddressType.HOME;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum AddressType {
        HOME, WORK, OTHER
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
