package com.example.ecommerce.event;

import java.math.BigDecimal;

public record PaymentCompletedEvent(
    Long paymentId,
    Long orderId,
    Long buyerId,
    BigDecimal amount,
    String transactionId
) {}
