package com.example.demo.dto;

import com.example.demo.model.User.UserStatus;
import jakarta.validation.constraints.*;

public record UserRequest(

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number")
        String phone,

        @NotNull(message = "Age is required")
        @Min(value = 0, message = "Age cannot be negative")
        @Max(value = 150, message = "Age must be ≤ 150")
        Integer age,

        @NotNull(message = "Status is required")
        UserStatus status
) {}
