package com.example.ecommerce.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class CartDTOs {
    
    public record AddToCartRequest(
        @NotNull(message = "Product ID is required")
        Long productId,
        
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 1000, message = "Quantity cannot exceed 1000")
        Integer quantity
    ) {}
    
    public record UpdateCartItemRequest(
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 1000, message = "Quantity cannot exceed 1000")
        Integer quantity
    ) {}
    
    public record CartItemResponse(
        Long cartItemId,
        Long productId,
        String productName,
        String productStatus,
        Integer quantity,
        BigDecimal priceAtAdd,
        BigDecimal subtotal
    ) {}
    
    public record CartResponse(
        Long cartId,
        Long userId,
        java.util.List<CartItemResponse> items,
        BigDecimal totalAmount,
        Integer itemCount,
        String expiresAt
    ) {}
}
