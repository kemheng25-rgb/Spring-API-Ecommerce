package com.example.ecommerce.repository;

import com.example.ecommerce.model.Dispute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    
    Optional<Dispute> findByDisputeNumber(String disputeNumber);
    
    Optional<Dispute> findByOrderId(Long orderId);
    
    Page<Dispute> findByBuyerId(Long buyerId, Pageable pageable);
    
    Page<Dispute> findBySellerId(Long sellerId, Pageable pageable);
    
    Page<Dispute> findByDisputeStatus(Dispute.DisputeStatus status, Pageable pageable);
    
    Page<Dispute> findByAssignedAdminId(Long adminId, Pageable pageable);
    
    Page<Dispute> findByDisputeStatusOrderByCreatedAtDesc(Dispute.DisputeStatus status, Pageable pageable);
    
    long countByBuyerId(Long buyerId);
    
    long countBySellerId(Long sellerId);
    
    long countByDisputeStatus(Dispute.DisputeStatus status);
    
    long countByAssignedAdminIdIsNull();
}
