package com.example.ecommerce.repository;

import com.example.ecommerce.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    Optional<Review> findByProductIdAndBuyerId(Long productId, Long buyerId);
    
    Page<Review> findByProductIdAndReviewStatusOrderByCreatedAtDesc(Long productId, Review.ReviewStatus status, Pageable pageable);
    
    Page<Review> findByProductIdAndReviewStatusOrderByRatingDesc(Long productId, Review.ReviewStatus status, Pageable pageable);
    
    Page<Review> findByBuyerId(Long buyerId, Pageable pageable);

    Page<Review> findByReviewStatusOrderByCreatedAtDesc(Review.ReviewStatus status, Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.product.seller.id = :sellerId ORDER BY r.createdAt DESC")
    Page<Review> findBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);
    
    Page<Review> findByProductIdAndIsLockedFalseOrderByHelpfulCountDesc(Long productId, Pageable pageable);
    
    @Query("SELECT r FROM Review r WHERE r.product.id = :productId AND r.reviewStatus = :status AND r.isLocked = false ORDER BY r.helpfulCount DESC, r.createdAt DESC")
    Page<Review> findMostHelpfulReviews(@Param("productId") Long productId, 
                                        @Param("status") Review.ReviewStatus status, Pageable pageable);
    
    @Query("SELECT r FROM Review r WHERE r.reviewStatus = :status AND r.createdAt > :sinceDate ORDER BY r.createdAt DESC")
    Page<Review> findRecentReviews(@Param("status") Review.ReviewStatus status, 
                                   @Param("sinceDate") LocalDateTime sinceDate, Pageable pageable);
    
    long countByProductId(Long productId);
    
    long countByProductIdAndReviewStatus(Long productId, Review.ReviewStatus status);
    
    long countByBuyerId(Long buyerId);
}
