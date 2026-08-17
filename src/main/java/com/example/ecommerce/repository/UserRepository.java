package com.example.ecommerce.repository;

import com.example.ecommerce.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByEmailAndDeletedAtIsNull(String email);
    
    Page<User> findByIsSeller(Boolean isSeller, Pageable pageable);

    Page<User> findByIsBuyer(Boolean isBuyer, Pageable pageable);

    Page<User> findByAccountStatus(User.AccountStatus status, Pageable pageable);
    
    Page<User> findByAccountStatusAndIsSeller(User.AccountStatus status, Boolean isSeller, Pageable pageable);
    
    long countByIsSeller(Boolean isSeller);
    
    long countByAccountStatus(User.AccountStatus status);
}
