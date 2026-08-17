package com.example.ecommerce.repository;

import com.example.ecommerce.model.CartItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    
    Optional<CartItem> findByIdAndCartId(Long id, Long cartId);
    
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
    
    Page<CartItem> findByCartId(Long cartId, Pageable pageable);
    
    long countByCartId(Long cartId);
    
    void deleteByCartId(Long cartId);
}
