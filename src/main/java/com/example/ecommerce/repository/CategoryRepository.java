package com.example.ecommerce.repository;

import com.example.ecommerce.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    Optional<Category> findByIdAndIsActive(Long id, Boolean isActive);
    
    List<Category> findByParentCategoryIsNullAndIsActive(Boolean isActive);
    
    Page<Category> findByParentCategoryIdAndIsActive(Long parentId, Boolean isActive, Pageable pageable);
    
    Page<Category> findByIsActive(Boolean isActive, Pageable pageable);
    
    Optional<Category> findByCategoryNameAndParentCategoryId(String categoryName, Long parentId);
    
    long countByParentCategoryId(Long parentId);
}
