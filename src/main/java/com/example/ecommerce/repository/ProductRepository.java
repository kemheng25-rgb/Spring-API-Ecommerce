package com.example.ecommerce.repository;

import com.example.ecommerce.model.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    /**
     * Row-locking read for the checkout stock decrement (Constraint 1 / Rule 1: "stock quantity
     * must never go below 0"). Mirrors the schema's `SELECT ... FOR UPDATE` - must only be called
     * inside an existing @Transactional method, never on its own.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
    
    Optional<Product> findByIdAndProductStatus(Long id, Product.ProductStatus status);
    
    Page<Product> findByProductStatusAndCategoryId(Product.ProductStatus status, Long categoryId, Pageable pageable);
    
    Page<Product> findBySellerId(Long sellerId, Pageable pageable);
    
    Page<Product> findBySellerIdAndProductStatus(Long sellerId, Product.ProductStatus status, Pageable pageable);
    
    Page<Product> findByProductStatusOrderByAverageRatingDesc(Product.ProductStatus status, Pageable pageable);
    
    Page<Product> findByProductStatusOrderByCreatedAtDesc(Product.ProductStatus status, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.productStatus = :status AND p.category.id = :categoryId ORDER BY p.averageRating DESC")
    Page<Product> findBestRatedInCategory(@Param("status") Product.ProductStatus status,
                                          @Param("categoryId") Long categoryId, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.productStatus = :status AND p.discountPercentage > 0 AND p.discountValidUntil > :now ORDER BY p.discountPercentage DESC")
    Page<Product> findActiveDiscountedProducts(@Param("status") Product.ProductStatus status, 
                                               @Param("now") LocalDateTime now, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.productStatus = :status AND p.stockQuantity <= :threshold ORDER BY p.stockQuantity ASC")
    Page<Product> findLowStockProducts(@Param("status") Product.ProductStatus status, 
                                       @Param("threshold") Integer threshold, Pageable pageable);
    
    long countBySellerId(Long sellerId);
    
    long countByProductStatus(Product.ProductStatus status);
}
