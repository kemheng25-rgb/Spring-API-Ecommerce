package com.example.ecommerce.exception;

public class OutOfStockException extends RuntimeException {
    
    private final Long productId;
    private final Integer requestedQuantity;
    private final Integer availableQuantity;
    
    public OutOfStockException(Long productId, Integer requestedQuantity, Integer availableQuantity) {
        super(String.format("Product with ID %d is out of stock. Requested: %d, Available: %d", 
                            productId, requestedQuantity, availableQuantity));
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }
    
    public OutOfStockException(String message) {
        super(message);
        this.productId = null;
        this.requestedQuantity = null;
        this.availableQuantity = null;
    }
    
    public Long getProductId() {
        return productId;
    }
    
    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }
    
    public Integer getAvailableQuantity() {
        return availableQuantity;
    }
}
