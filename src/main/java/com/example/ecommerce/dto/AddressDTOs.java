package com.example.ecommerce.dto;

import jakarta.validation.constraints.*;

public class AddressDTOs {
    
    public record AddressCreateRequest(
        @NotBlank(message = "Full name is required")
        String fullName,
        
        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Phone number must be valid")
        String phone,
        
        @NotBlank(message = "Street address is required")
        String streetAddress,
        
        @NotBlank(message = "City is required")
        String city,
        
        @NotBlank(message = "State/Province is required")
        String stateProvince,
        
        @NotBlank(message = "Postal code is required")
        String postalCode,
        
        @NotBlank(message = "Country is required")
        String country,
        
        Boolean isDefault,
        
        String addressType
    ) {}
    
    public record AddressUpdateRequest(
        @NotBlank(message = "Full name is required")
        String fullName,
        
        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Phone number must be valid")
        String phone,
        
        @NotBlank(message = "Street address is required")
        String streetAddress,
        
        @NotBlank(message = "City is required")
        String city,
        
        @NotBlank(message = "State/Province is required")
        String stateProvince,
        
        @NotBlank(message = "Postal code is required")
        String postalCode,
        
        @NotBlank(message = "Country is required")
        String country,
        
        Boolean isDefault
    ) {}
    
    public record AddressResponse(
        Long id,
        String fullName,
        String phone,
        String streetAddress,
        String city,
        String stateProvince,
        String postalCode,
        String country,
        Boolean isDefault,
        String addressType
    ) {}
}
