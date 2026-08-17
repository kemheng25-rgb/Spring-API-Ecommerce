package com.example.demo.dto;

import com.example.demo.model.User.UserStatus;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        String phone,
        Integer age,
        UserStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
