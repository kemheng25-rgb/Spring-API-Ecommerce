package com.example.ecommerce.controller;

import com.example.ecommerce.dto.CartDTOs;
import com.example.ecommerce.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/{userId}/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart, one active cart per user")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get the buyer's cart")
    public ResponseEntity<CartDTOs.CartResponse> getCart(@PathVariable Long userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/items")
    @Operation(summary = "Add a product to the cart (merges quantity if already present)")
    public ResponseEntity<CartDTOs.CartResponse> addItem(
            @PathVariable Long userId, @Valid @RequestBody CartDTOs.AddToCartRequest request) {
        return ResponseEntity.ok(cartService.addToCart(userId, request));
    }

    @PutMapping("/items/{cartItemId}")
    @Operation(summary = "Change the quantity of a cart line item")
    public ResponseEntity<CartDTOs.CartResponse> updateItem(
            @PathVariable Long userId, @PathVariable Long cartItemId,
            @Valid @RequestBody CartDTOs.UpdateCartItemRequest request) {
        return ResponseEntity.ok(cartService.updateCartItem(userId, cartItemId, request));
    }

    @DeleteMapping("/items/{cartItemId}")
    @Operation(summary = "Remove a line item from the cart")
    public ResponseEntity<CartDTOs.CartResponse> removeItem(@PathVariable Long userId, @PathVariable Long cartItemId) {
        return ResponseEntity.ok(cartService.removeFromCart(userId, cartItemId));
    }

    @DeleteMapping
    @Operation(summary = "Empty the cart")
    public ResponseEntity<Void> clear(@PathVariable Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}
