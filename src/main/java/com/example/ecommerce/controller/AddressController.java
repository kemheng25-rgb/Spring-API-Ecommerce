package com.example.ecommerce.controller;

import com.example.ecommerce.dto.AddressDTOs;
import com.example.ecommerce.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/{userId}/addresses")
@RequiredArgsConstructor
@Tag(name = "Addresses", description = "Buyer shipping/billing addresses")
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    @Operation(summary = "Add an address (first address, or isDefault=true, becomes the default)")
    public ResponseEntity<AddressDTOs.AddressResponse> create(
            @PathVariable Long userId, @Valid @RequestBody AddressDTOs.AddressCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.createAddress(userId, request));
    }

    @GetMapping
    @Operation(summary = "List a user's addresses, default first")
    public ResponseEntity<List<AddressDTOs.AddressResponse>> list(@PathVariable Long userId) {
        return ResponseEntity.ok(addressService.getAddresses(userId));
    }

    @GetMapping("/{addressId}")
    @Operation(summary = "Get one address")
    public ResponseEntity<AddressDTOs.AddressResponse> get(@PathVariable Long userId, @PathVariable Long addressId) {
        return ResponseEntity.ok(addressService.getAddress(userId, addressId));
    }

    @PutMapping("/{addressId}")
    @Operation(summary = "Update an address")
    public ResponseEntity<AddressDTOs.AddressResponse> update(
            @PathVariable Long userId, @PathVariable Long addressId, @Valid @RequestBody AddressDTOs.AddressUpdateRequest request) {
        return ResponseEntity.ok(addressService.updateAddress(userId, addressId, request));
    }

    @DeleteMapping("/{addressId}")
    @Operation(summary = "Delete an address (promotes another one to default if this was it)")
    public ResponseEntity<Void> delete(@PathVariable Long userId, @PathVariable Long addressId) {
        addressService.deleteAddress(userId, addressId);
        return ResponseEntity.noContent().build();
    }
}
