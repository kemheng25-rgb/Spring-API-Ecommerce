package com.example.ecommerce.repository;

import com.example.ecommerce.model.SellerProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SellerProfileRepository extends JpaRepository<SellerProfile, Long> {
    
    Optional<SellerProfile> findByUserId(Long userId);
    
    Optional<SellerProfile> findByShopName(String shopName);
    
    Page<SellerProfile> findByVerificationStatus(SellerProfile.VerificationStatus status, Pageable pageable);
    
    Page<SellerProfile> findByOrderBySellerRatingDesc(Pageable pageable);
    
    Page<SellerProfile> findBySellerRatingGreaterThanEqualOrderBySellerRatingDesc(java.math.BigDecimal minRating, Pageable pageable);
    
    long countByVerificationStatus(SellerProfile.VerificationStatus status);
}
