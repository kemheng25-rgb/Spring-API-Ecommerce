package com.example.ecommerce.dto;

import java.math.BigDecimal;

public class SellerLedgerDTOs {

    public record LedgerEntryResponse(
        Long entryId,
        String entryType,
        String description,
        Long orderItemId,
        Long orderId,
        String orderNumber,
        String orderDate,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal grossAmount,
        BigDecimal commissionRate,
        BigDecimal commissionAmount,
        BigDecimal netAmount,
        String itemStatus,
        String paymentStatus,
        String refundStatus,
        String createdAt
    ) {}

    public record LedgerSummaryResponse(
        BigDecimal commissionRate,
        Long unitsSold,
        BigDecimal availableGross,
        BigDecimal availableNet,
        BigDecimal pendingGross,
        BigDecimal pendingNet
    ) {}
}
