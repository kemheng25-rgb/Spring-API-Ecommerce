package com.example.ecommerce.event;

import java.math.BigDecimal;
import java.util.List;

public record PaymentCompletedEvent(
    Long paymentId,
    Long orderId,
    String orderNumber,
    Long buyerId,
    String buyerName,
    BigDecimal amount,
    String paymentMethod,
    String transactionId,
    List<Item> items
) {
    public record Item(String productName, Integer quantity, BigDecimal unitPrice, BigDecimal subtotal) {}
}
