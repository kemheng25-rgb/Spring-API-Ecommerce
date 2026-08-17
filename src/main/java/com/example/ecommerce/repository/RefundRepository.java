package com.example.ecommerce.repository;

import com.example.ecommerce.model.Refund;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
    
    Optional<Refund> findByRefundTransactionId(String refundTransactionId);
    
    List<Refund> findByPaymentId(Long paymentId);
    
    Page<Refund> findByPaymentId(Long paymentId, Pageable pageable);
    
    Page<Refund> findByRefundStatus(Refund.RefundStatus status, Pageable pageable);
    
    Page<Refund> findByRefundStatusOrderByRefundDateDesc(Refund.RefundStatus status, Pageable pageable);
    
    long countByPaymentId(Long paymentId);
    
    long countByRefundStatus(Refund.RefundStatus status);
}
