package com.example.ecommerce.repository;

import com.example.ecommerce.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    Page<Order> findByBuyerIdOrderByOrderDateDesc(Long buyerId, Pageable pageable);

    // Not a derived query: Order.seller is a separate, always-null field (reserved for a
    // single-seller-order optimization that was never wired up) - seller ownership actually
    // lives on OrderItem.seller, since one order can span multiple sellers. A derived
    // "findBySellerId" here would silently compile against Order.seller and never match anything.
    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i WHERE i.seller.id = :sellerId ORDER BY o.orderDate DESC")
    Page<Order> findBySellerIdOrderByOrderDateDesc(@Param("sellerId") Long sellerId, Pageable pageable);

    Page<Order> findByOrderStatusOrderByOrderDateDesc(Order.OrderStatus status, Pageable pageable);
    
    Page<Order> findByBuyerIdAndOrderStatusOrderByOrderDateDesc(Long buyerId, Order.OrderStatus status, Pageable pageable);
    
    Page<Order> findBySellerIdAndOrderStatusOrderByOrderDateDesc(Long sellerId, Order.OrderStatus status, Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE o.orderStatus = :status AND o.orderDate < :cutoffDate")
    Page<Order> findUnprocessedOrders(@Param("status") Order.OrderStatus status, 
                                       @Param("cutoffDate") LocalDateTime cutoffDate, Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE o.returnInitiatedAt IS NOT NULL AND o.orderStatus = :status")
    Page<Order> findOrdersWithReturnRequests(@Param("status") Order.OrderStatus status, Pageable pageable);
    
    long countByBuyerId(Long buyerId);
    
    long countBySellerId(Long sellerId);
    
    long countByOrderStatus(Order.OrderStatus status);
}
