package com.example.ecommerce.event;

import java.math.BigDecimal;

public record PaymentCompletedEvent(
    Long paymentId,
    Long orderId,
    String orderNumber,
    Long buyerId,
    String buyerName,
    BigDecimal amount,
    String transactionId
) {}
