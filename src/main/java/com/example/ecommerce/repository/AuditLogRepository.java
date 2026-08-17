package com.example.ecommerce.repository;

import com.example.ecommerce.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    Page<AuditLog> findByUserId(Long userId, Pageable pageable);
    
    Page<AuditLog> findByAction(String action, Pageable pageable);
    
    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);
    
    Page<AuditLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    Page<AuditLog> findByUserIdAndTimestampBetweenOrderByTimestampDesc(Long userId, LocalDateTime startTime, 
                                                                        LocalDateTime endTime, Pageable pageable);
    
    Page<AuditLog> findByEntityTypeAndTimestampBetweenOrderByTimestampDesc(String entityType, LocalDateTime startTime, 
                                                                            LocalDateTime endTime, Pageable pageable);
    
    long countByUserId(Long userId);
    
    long countByAction(String action);
}
