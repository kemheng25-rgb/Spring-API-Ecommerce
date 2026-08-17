package com.example.ecommerce.dto;

import jakarta.validation.constraints.*;

public class DisputeDTOs {
    
    public record CreateDisputeRequest(
        @NotNull(message = "Order ID is required")
        Long orderId,
        
        @NotBlank(message = "Dispute reason is required")
        String disputeReason,
        
        @NotBlank(message = "Description is required")
        @Size(min = 20, max = 5000, message = "Description must be between 20 and 5000 characters")
        String description,
        
        String evidenceUrls
    ) {}
    
    public record ResolveDisputeRequest(
        @NotBlank(message = "Resolution is required")
        String resolution,
        
        @NotBlank(message = "Admin notes are required")
        @Size(min = 10, max = 2000, message = "Admin notes must be between 10 and 2000 characters")
        String adminNotes
    ) {}
    
    public record DisputeResponse(
        Long disputeId,
        String disputeNumber,
        Long orderId,
        String orderNumber,
        String initiatedBy,
        String disputeReason,
        String description,
        String disputeStatus,
        String resolution,
        String assignedAdminName,
        String adminNotes,
        String resolutionNotes,
        String createdAt,
        String resolvedAt
    ) {}
    
    public record DisputeListResponse(
        Long disputeId,
        String disputeNumber,
        String disputeReason,
        String disputeStatus,
        String createdAt,
        String buyerName,
        String sellerName
    ) {}
}
