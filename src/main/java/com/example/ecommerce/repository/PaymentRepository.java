package com.example.ecommerce.repository;

import com.example.ecommerce.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    Optional<Payment> findByOrderId(Long orderId);
    
    Optional<Payment> findByTransactionId(String transactionId);
    
    Page<Payment> findByBuyerId(Long buyerId, Pageable pageable);
    
    Page<Payment> findByPaymentStatus(Payment.PaymentStatus status, Pageable pageable);
    
    Page<Payment> findByBuyerIdAndPaymentStatus(Long buyerId, Payment.PaymentStatus status, Pageable pageable);
    
    Page<Payment> findByPaymentStatusOrderByPaymentDateDesc(Payment.PaymentStatus status, Pageable pageable);
    
    List<Payment> findByRefundStatusNotAndPaymentStatus(Payment.RefundStatus refundStatus, 
                                                         Payment.PaymentStatus paymentStatus);
    
    long countByPaymentStatus(Payment.PaymentStatus status);
    
    long countByBuyerId(Long buyerId);
}
