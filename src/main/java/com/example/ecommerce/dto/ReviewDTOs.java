package com.example.ecommerce.dto;

import jakarta.validation.constraints.*;

public class ReviewDTOs {
    
    public record CreateReviewRequest(
        @NotNull(message = "Product ID is required")
        Long productId,
        
        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be between 1 and 5")
        @Max(value = 5, message = "Rating must be between 1 and 5")
        Integer rating,
        
        @Size(max = 200, message = "Review title cannot exceed 200 characters")
        String reviewTitle,
        
        @NotBlank(message = "Review comment is required")
        @Size(min = 10, max = 5000, message = "Review comment must be between 10 and 5000 characters")
        String reviewComment
    ) {}
    
    public record UpdateReviewRequest(
        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be between 1 and 5")
        @Max(value = 5, message = "Rating must be between 1 and 5")
        Integer rating,
        
        @Size(max = 200, message = "Review title cannot exceed 200 characters")
        String reviewTitle,
        
        @NotBlank(message = "Review comment is required")
        @Size(min = 10, max = 5000, message = "Review comment must be between 10 and 5000 characters")
        String reviewComment
    ) {}
    
    public record ReviewResponse(
        Long reviewId,
        Long productId,
        String productName,
        Long buyerId,
        String buyerName,
        Integer rating,
        String reviewTitle,
        String reviewComment,
        Boolean verifiedPurchase,
        Integer helpfulCount,
        String reviewStatus,
        Boolean isLocked,
        String sellerResponse,
        String sellerResponseAt,
        String createdAt
    ) {}
    
    public record ReviewListResponse(
        Long reviewId,
        Integer rating,
        String reviewTitle,
        String reviewComment,
        Boolean verifiedPurchase,
        Integer helpfulCount,
        String buyerName,
        String createdAt,
        String sellerResponse,
        String sellerResponseAt
    ) {}
    
    public record SellerResponseRequest(
        @NotBlank(message = "Response is required")
        @Size(min = 10, max = 2000, message = "Response must be between 10 and 2000 characters")
        String response
    ) {}
}
