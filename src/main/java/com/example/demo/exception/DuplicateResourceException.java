package com.example.demo.exception;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String resource, String field, String value) {
        super("%s already exists with %s: %s".formatted(resource, field, value));
    }
}
