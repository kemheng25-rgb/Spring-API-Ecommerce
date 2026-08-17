package com.example.ecommerce.repository;

import com.example.ecommerce.model.Address;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    
    Optional<Address> findByUserIdAndIsDefault(Long userId, Boolean isDefault);
    
    Page<Address> findByUserId(Long userId, Pageable pageable);
    
    List<Address> findByUserIdOrderByIsDefaultDesc(Long userId);
    
    Optional<Address> findByIdAndUserId(Long id, Long userId);
    
    long countByUserId(Long userId);
}
