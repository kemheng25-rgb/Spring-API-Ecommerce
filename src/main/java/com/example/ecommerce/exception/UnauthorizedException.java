package com.example.ecommerce.exception;

public class UnauthorizedException extends RuntimeException {
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    public UnauthorizedException(String resourceName, Long userId) {
        super(String.format("User %d is not authorized to access %s", userId, resourceName));
    }
}
