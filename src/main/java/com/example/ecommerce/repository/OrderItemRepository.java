package com.example.ecommerce.repository;

import com.example.ecommerce.model.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    Optional<OrderItem> findByOrderIdAndProductId(Long orderId, Long productId);

    /** Rule 4 (Review Eligibility): "must have completed (delivered) order for that product". */
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.buyer.id = :buyerId AND oi.product.id = :productId " +
           "AND oi.itemStatus = 'DELIVERED' ORDER BY oi.updatedAt DESC")
    List<OrderItem> findDeliveredByBuyerAndProduct(@Param("buyerId") Long buyerId, @Param("productId") Long productId);
    
    Page<OrderItem> findByOrderId(Long orderId, Pageable pageable);
    
    Page<OrderItem> findBySellerIdAndItemStatus(Long sellerId, OrderItem.ItemStatus status, Pageable pageable);
    
    @Query("SELECT oi FROM OrderItem oi WHERE oi.seller.id = :sellerId AND oi.itemStatus = :status ORDER BY oi.createdAt DESC")
    Page<OrderItem> findSellerOrderItems(@Param("sellerId") Long sellerId, 
                                         @Param("status") OrderItem.ItemStatus status, Pageable pageable);
    
    long countByOrderId(Long orderId);

    long countBySellerIdAndItemStatus(Long sellerId, OrderItem.ItemStatus status);

    /** Seller earnings ledger units-sold metric (see SellerLedgerService) - a fulfillment count, not a monetary figure. */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE oi.seller.id = :sellerId AND oi.itemStatus = :status")
    Long sumQuantityBySellerIdAndItemStatus(@Param("sellerId") Long sellerId, @Param("status") OrderItem.ItemStatus status);
}
