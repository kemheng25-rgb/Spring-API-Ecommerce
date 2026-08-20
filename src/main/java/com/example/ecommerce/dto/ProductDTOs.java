package com.example.ecommerce.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class ProductDTOs {
    
    public record ProductCreateRequest(
        @NotBlank(message = "Product name is required")
        String productName,
        
        @NotBlank(message = "Product description is required")
        String productDescription,
        
        @NotBlank(message = "SKU is required")
        String sku,
        
        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        BigDecimal price,
        
        @NotNull(message = "Stock quantity is required")
        @Min(value = 0, message = "Stock quantity cannot be negative")
        Integer stockQuantity,
        
        @NotNull(message = "Category ID is required")
        Long categoryId,
        
        @Min(value = 0, message = "Discount percentage cannot be negative")
        @Max(value = 100, message = "Discount percentage cannot exceed 100")
        BigDecimal discountPercentage
    ) {}
    
    public record ProductUpdateRequest(
        @NotBlank(message = "Product name is required")
        String productName,
        
        @NotBlank(message = "Product description is required")
        String productDescription,
        
        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        BigDecimal price,
        
        @NotNull(message = "Stock quantity is required")
        @Min(value = 0, message = "Stock quantity cannot be negative")
        Integer stockQuantity,
        
        @Min(value = 0, message = "Discount percentage cannot be negative")
        @Max(value = 100, message = "Discount percentage cannot exceed 100")
        BigDecimal discountPercentage,
        
        String productStatus
    ) {}
    
    public record ProductResponse(
        Long id,
        String productName,
        String productDescription,
        String sku,
        BigDecimal price,
        Integer stockQuantity,
        String productStatus,
        BigDecimal averageRating,
        Integer totalReviews,
        BigDecimal discountPercentage,
        Long categoryId,
        Long sellerId,
        String sellerName,
        Integer viewsCount,
        String createdAt,
        String primaryImageUrl
    ) {}

    public record ProductListResponse(
        Long id,
        String productName,
        BigDecimal price,
        String productStatus,
        BigDecimal averageRating,
        Integer totalReviews,
        BigDecimal discountPercentage,
        String categoryName,
        String sellerName,
        String primaryImageUrl
    ) {}
}
