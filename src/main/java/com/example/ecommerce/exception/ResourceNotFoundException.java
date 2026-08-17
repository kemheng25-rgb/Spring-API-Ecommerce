package com.example.ecommerce.exception;

public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
    }
    
    public ResourceNotFoundException(String resourceName, Long id) {
        super(String.format("%s not found with ID: %d", resourceName, id));
    }
}
