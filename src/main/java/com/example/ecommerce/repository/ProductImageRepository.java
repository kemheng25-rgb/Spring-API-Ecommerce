package com.example.ecommerce.repository;

import com.example.ecommerce.model.ProductImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    
    List<ProductImage> findByProductIdOrderByDisplayOrder(Long productId);
    
    Page<ProductImage> findByProductId(Long productId, Pageable pageable);
    
    long countByProductId(Long productId);
    
    void deleteByProductId(Long productId);
}
