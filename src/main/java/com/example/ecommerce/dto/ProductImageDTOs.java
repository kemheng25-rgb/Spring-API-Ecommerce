package com.example.ecommerce.dto;

import jakarta.validation.constraints.*;

public class ProductImageDTOs {

    public record ProductImageCreateRequest(
        @NotBlank(message = "Image URL is required")
        @Size(max = 500, message = "Image URL cannot exceed 500 characters")
        String imageUrl,

        @Size(max = 200, message = "Alt text cannot exceed 200 characters")
        String altText,

        @Min(value = 0, message = "Display order must be non-negative")
        Integer displayOrder
    ) {}

    public record ProductImageReorderRequest(
        @NotNull(message = "Display order is required")
        @Min(value = 0, message = "Display order must be non-negative")
        Integer displayOrder
    ) {}

    public record ProductImageResponse(
        Long id,
        Long productId,
        String imageUrl,
        String altText,
        Integer displayOrder,
        String uploadedAt
    ) {}
}
