package com.example.ecommerce.exception;

public class PaymentFailedException extends RuntimeException {
    
    private final String transactionId;
    private final String reason;
    
    public PaymentFailedException(String message) {
        super(message);
        this.transactionId = null;
        this.reason = null;
    }
    
    public PaymentFailedException(String transactionId, String reason) {
        super(String.format("Payment failed for transaction %s. Reason: %s", transactionId, reason));
        this.transactionId = transactionId;
        this.reason = reason;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public String getReason() {
        return reason;
    }
}
