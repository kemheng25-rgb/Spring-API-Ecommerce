package com.example.ecommerce.dto;

import jakarta.validation.constraints.*;

public class CategoryDTOs {
    
    public record CategoryCreateRequest(
        @NotBlank(message = "Category name is required")
        String categoryName,
        
        String categoryDescription,
        
        Long parentId,
        
        @Min(value = 0, message = "Display order must be non-negative")
        Integer displayOrder
    ) {}
    
    public record CategoryUpdateRequest(
        @NotBlank(message = "Category name is required")
        String categoryName,
        
        String categoryDescription,
        
        @Min(value = 0, message = "Display order must be non-negative")
        Integer displayOrder,
        
        Boolean isActive
    ) {}
    
    public record CategoryResponse(
        Long id,
        Long parentId,
        String categoryName,
        String categoryDescription,
        Boolean isActive,
        Integer displayOrder,
        Integer productCount,
        String createdAt
    ) {}
}
