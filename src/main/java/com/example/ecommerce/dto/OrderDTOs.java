package com.example.ecommerce.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public class OrderDTOs {
    
    public record PlaceOrderRequest(
        @NotNull(message = "Shipping address ID is required")
        Long shippingAddressId,
        
        Long billingAddressId,
        
        @NotBlank(message = "Shipping method is required")
        String shippingMethod
    ) {}
    
    public record OrderItemResponse(
        Long orderItemId,
        Long productId,
        String productName,
        String sellerName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        String itemStatus
    ) {}
    
    public record OrderResponse(
        Long orderId,
        String orderNumber,
        String orderStatus,
        BigDecimal totalAmount,
        String shippingMethod,
        String trackingNumber,
        String estimatedDeliveryDate,
        String actualDeliveryDate,
        String orderDate,
        String updatedAt,
        List<OrderItemResponse> items
    ) {}
    
    public record OrderListResponse(
        Long orderId,
        String orderNumber,
        String orderStatus,
        BigDecimal totalAmount,
        Integer itemCount,
        String orderDate
    ) {}
    
    public record CancelOrderRequest(
        @NotBlank(message = "Cancellation reason is required")
        String reason
    ) {}
    
    public record InitiateReturnRequest(
        @NotNull(message = "Order item ID is required")
        Long orderItemId,
        
        @NotBlank(message = "Return reason is required")
        String reason,
        
        String description
    ) {}
}
