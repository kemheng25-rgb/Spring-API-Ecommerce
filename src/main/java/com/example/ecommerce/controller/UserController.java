package com.example.ecommerce.controller;

import com.example.ecommerce.dto.UserDTOs;
import com.example.ecommerce.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Registration, login and account management")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "Register a new buyer account")
    public ResponseEntity<UserDTOs.UserResponse> register(@Valid @RequestBody UserDTOs.UserRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.registerUser(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with email and password")
    public ResponseEntity<UserDTOs.UserResponse> login(@Valid @RequestBody UserDTOs.UserLoginRequest request) {
        return ResponseEntity.ok(userService.authenticate(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user by ID")
    public ResponseEntity<UserDTOs.UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping(params = "email")
    @Operation(summary = "Get a user by email")
    public ResponseEntity<UserDTOs.UserResponse> getByEmail(@RequestParam String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @PutMapping("/{id}/profile")
    @Operation(summary = "Update a user's profile (name, phone)")
    public ResponseEntity<UserDTOs.UserResponse> updateProfile(
            @PathVariable Long id, @Valid @RequestBody UserDTOs.UserProfileUpdateRequest request) {
        return ResponseEntity.ok(userService.updateUserProfile(id, request));
    }

    @GetMapping("/sellers")
    @Operation(summary = "List users who have a seller role enabled")
    public ResponseEntity<Page<UserDTOs.UserResponse>> getSellers(Pageable pageable) {
        return ResponseEntity.ok(userService.getSellers(pageable));
    }

    @GetMapping("/buyers")
    @Operation(summary = "List users who have a buyer role")
    public ResponseEntity<Page<UserDTOs.UserResponse>> getBuyers(Pageable pageable) {
        return ResponseEntity.ok(userService.getBuyers(pageable));
    }

    @PostMapping("/{id}/seller-role")
    @Operation(summary = "Enable the seller role for a user (usually set by SellerProfileService.applyAsSeller)")
    public ResponseEntity<Void> enableSellerRole(@PathVariable Long id) {
        userService.enableSellerRole(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/suspend")
    @Operation(summary = "Admin: suspend a user account")
    public ResponseEntity<Void> suspend(
            @PathVariable Long id, @RequestParam Long adminUserId, @RequestParam(required = false) String reason) {
        userService.suspendUser(id, adminUserId, reason);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a user account")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
