package com.example.ecommerce.repository;

import com.example.ecommerce.model.OrderItem;
import com.example.ecommerce.model.SellerLedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface SellerLedgerEntryRepository extends JpaRepository<SellerLedgerEntry, Long> {

    Page<SellerLedgerEntry> findBySellerIdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);

    List<SellerLedgerEntry> findByOrderItemIdOrderByCreatedAtAsc(Long orderItemId);

    List<SellerLedgerEntry> findByOrderItemIdAndEntryType(Long orderItemId, SellerLedgerEntry.EntryType entryType);

    @Query("SELECT COALESCE(SUM(e.netAmount), 0) FROM SellerLedgerEntry e " +
        "WHERE e.seller.id = :sellerId AND e.orderItem.itemStatus = :status")
    BigDecimal sumNetBySellerIdAndItemStatus(@Param("sellerId") Long sellerId, @Param("status") OrderItem.ItemStatus status);

    @Query("SELECT COALESCE(SUM(e.grossAmount), 0) FROM SellerLedgerEntry e " +
        "WHERE e.seller.id = :sellerId AND e.orderItem.itemStatus = :status")
    BigDecimal sumGrossBySellerIdAndItemStatus(@Param("sellerId") Long sellerId, @Param("status") OrderItem.ItemStatus status);

    @Query("SELECT COALESCE(SUM(e.netAmount), 0) FROM SellerLedgerEntry e " +
        "WHERE e.seller.id = :sellerId AND e.orderItem.itemStatus IN :statuses")
    BigDecimal sumNetBySellerIdAndItemStatusIn(@Param("sellerId") Long sellerId, @Param("statuses") Collection<OrderItem.ItemStatus> statuses);

    @Query("SELECT COALESCE(SUM(e.grossAmount), 0) FROM SellerLedgerEntry e " +
        "WHERE e.seller.id = :sellerId AND e.orderItem.itemStatus IN :statuses")
    BigDecimal sumGrossBySellerIdAndItemStatusIn(@Param("sellerId") Long sellerId, @Param("statuses") Collection<OrderItem.ItemStatus> statuses);
}
