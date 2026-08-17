package com.example.ecommerce.event;

import java.math.BigDecimal;
import java.util.List;

public record OrderPlacedEvent(
    Long orderId,
    String orderNumber,
    Long buyerId,
    BigDecimal totalAmount,
    List<Item> items
) {
    public record Item(Long productId, String productName, Integer quantity) {}
}
