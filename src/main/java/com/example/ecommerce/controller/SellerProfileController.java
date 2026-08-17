package com.example.ecommerce.controller;

import com.example.ecommerce.dto.SellerProfileDTOs;
import com.example.ecommerce.service.SellerProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@Tag(name = "Seller Profiles", description = "Seller onboarding and admin verification (Workflow 1)")
public class SellerProfileController {

    private final SellerProfileService sellerProfileService;

    @PostMapping("/api/v1/users/{userId}/seller-profile")
    @Operation(summary = "Apply to become a seller (starts UNVERIFIED)")
    public ResponseEntity<SellerProfileDTOs.SellerProfileResponse> apply(
            @PathVariable Long userId, @Valid @RequestBody SellerProfileDTOs.SellerProfileCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sellerProfileService.applyAsSeller(userId, request));
    }

    @GetMapping("/api/v1/seller-profiles/{id}")
    @Operation(summary = "Get a seller profile")
    public ResponseEntity<SellerProfileDTOs.SellerProfileResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(sellerProfileService.getSellerProfile(id));
    }

    @GetMapping("/api/v1/users/{userId}/seller-profile")
    @Operation(summary = "Get a user's seller profile")
    public ResponseEntity<SellerProfileDTOs.SellerProfileResponse> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(sellerProfileService.getSellerProfileByUserId(userId));
    }

    @GetMapping("/api/v1/seller-profiles/pending")
    @Operation(summary = "Admin: sellers awaiting verification")
    public ResponseEntity<Page<SellerProfileDTOs.SellerProfileResponse>> pending(Pageable pageable) {
        return ResponseEntity.ok(sellerProfileService.getPendingVerification(pageable));
    }

    @GetMapping("/api/v1/seller-profiles/top-rated")
    @Operation(summary = "Top-rated sellers")
    public ResponseEntity<Page<SellerProfileDTOs.SellerProfileResponse>> topRated(Pageable pageable) {
        return ResponseEntity.ok(sellerProfileService.getTopRated(pageable));
    }

    @PutMapping("/api/v1/seller-profiles/{id}")
    @Operation(summary = "Update shop description/logo")
    public ResponseEntity<SellerProfileDTOs.SellerProfileResponse> update(
            @PathVariable Long id, @Valid @RequestBody SellerProfileDTOs.SellerProfileUpdateRequest request) {
        return ResponseEntity.ok(sellerProfileService.updateProfile(id, request));
    }

    @PostMapping("/api/v1/seller-profiles/{id}/verify")
    @Operation(summary = "Admin: approve a pending seller application")
    public ResponseEntity<SellerProfileDTOs.SellerProfileResponse> verify(@PathVariable Long id, @RequestParam Long adminUserId) {
        return ResponseEntity.ok(sellerProfileService.verifySeller(id, adminUserId));
    }

    @PostMapping("/api/v1/seller-profiles/{id}/reject")
    @Operation(summary = "Admin: reject a pending seller application")
    public ResponseEntity<SellerProfileDTOs.SellerProfileResponse> reject(@PathVariable Long id, @RequestParam Long adminUserId) {
        return ResponseEntity.ok(sellerProfileService.rejectSeller(id, adminUserId));
    }

    @PutMapping("/api/v1/seller-profiles/{id}/commission-rate")
    @Operation(summary = "Admin: set this seller's commission rate (Rule 9)")
    public ResponseEntity<SellerProfileDTOs.SellerProfileResponse> setCommissionRate(
            @PathVariable Long id, @RequestParam BigDecimal rate) {
        return ResponseEntity.ok(sellerProfileService.setCommissionRate(id, rate));
    }
}
