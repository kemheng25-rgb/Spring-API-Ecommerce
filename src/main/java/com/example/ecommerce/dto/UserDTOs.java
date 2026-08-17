package com.example.ecommerce.dto;

import jakarta.validation.constraints.*;

public class UserDTOs {
    
    public record UserRegistrationRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,
        
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,
        
        @NotBlank(message = "Full name is required")
        String fullName,
        
        @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Phone number must be valid")
        String phone
    ) {}
    
    public record UserLoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,
        
        @NotBlank(message = "Password is required")
        String password
    ) {}
    
    public record UserResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        String accountStatus,
        Boolean emailVerified,
        Boolean isBuyer,
        Boolean isSeller,
        String createdAt
    ) {}
    
    public record UserProfileUpdateRequest(
        @NotBlank(message = "Full name is required")
        String fullName,
        
        @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Phone number must be valid")
        String phone
    ) {}
}
