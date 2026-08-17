package com.example.ecommerce.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class PaymentDTOs {
    
    public record ProcessPaymentRequest(
        @NotNull(message = "Order ID is required")
        Long orderId,
        
        @NotBlank(message = "Payment method is required")
        String paymentMethod,
        
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount,
        
        String paymentGatewayToken
    ) {}
    
    public record PaymentResponse(
        Long paymentId,
        Long orderId,
        BigDecimal amount,
        String paymentMethod,
        String paymentStatus,
        String paymentDate,
        String refundStatus,
        BigDecimal totalRefundedAmount
    ) {}
    
    public record ProcessRefundRequest(
        @NotNull(message = "Payment ID is required")
        Long paymentId,
        
        @NotNull(message = "Refund amount is required")
        @DecimalMin(value = "0.01", message = "Refund amount must be greater than 0")
        BigDecimal refundAmount,
        
        @NotBlank(message = "Refund reason is required")
        String refundReason,
        
        String notes
    ) {}
    
    public record RefundResponse(
        Long refundId,
        Long paymentId,
        BigDecimal refundAmount,
        String refundReason,
        String refundStatus,
        String refundDate
    ) {}
}
