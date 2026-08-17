package com.example.demo.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        int status,
        String message,
        List<String> errors,
        LocalDateTime timestamp
) {

    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message, List.of(), LocalDateTime.now());
    }

    public static ErrorResponse of(int status, String message, List<String> errors) {
        return new ErrorResponse(status, message, errors, LocalDateTime.now());
    }
}
