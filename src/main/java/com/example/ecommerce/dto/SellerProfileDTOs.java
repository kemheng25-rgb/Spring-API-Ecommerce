package com.example.ecommerce.dto;

import jakarta.validation.constraints.*;

public class SellerProfileDTOs {
    
    public record SellerProfileCreateRequest(
        @NotBlank(message = "Shop name is required")
        String shopName,
        
        @NotBlank(message = "Shop description is required")
        @Size(min = 20, max = 2000, message = "Shop description must be between 20 and 2000 characters")
        String shopDescription,
        
        @Size(max = 500, message = "Logo URL cannot exceed 500 characters")
        String shopLogoUrl,
        
        @NotBlank(message = "Bank account is required")
        String bankAccountEncrypted
    ) {}
    
    public record SellerProfileUpdateRequest(
        @NotBlank(message = "Shop description is required")
        @Size(min = 20, max = 2000, message = "Shop description must be between 20 and 2000 characters")
        String shopDescription,
        
        @Size(max = 500, message = "Logo URL cannot exceed 500 characters")
        String shopLogoUrl
    ) {}
    
    public record SellerProfileResponse(
        Long sellerId,
        String shopName,
        String shopDescription,
        String shopLogoUrl,
        java.math.BigDecimal sellerRating,
        Integer totalProductsSold,
        java.math.BigDecimal totalRevenue,
        String verificationStatus,
        java.math.BigDecimal commissionRate,
        String shopCreatedAt
    ) {}
}
