package com.example.ecommerce.controller;

import com.example.ecommerce.dto.OrderDTOs;
import com.example.ecommerce.service.OrderService;
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
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Checkout, fulfillment and returns (Workflows 3 & 4)")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/api/v1/users/{buyerId}/orders")
    @Operation(summary = "Place an order from the buyer's cart")
    public ResponseEntity<OrderDTOs.OrderResponse> placeOrder(
            @PathVariable Long buyerId, @Valid @RequestBody OrderDTOs.PlaceOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(buyerId, request, idempotencyKey));
    }

    @GetMapping("/api/v1/orders/{orderId}")
    @Operation(summary = "Get an order")
    public ResponseEntity<OrderDTOs.OrderResponse> get(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    @GetMapping("/api/v1/users/{buyerId}/orders")
    @Operation(summary = "Buyer's order history, most recent first")
    public ResponseEntity<Page<OrderDTOs.OrderListResponse>> byBuyer(@PathVariable Long buyerId, Pageable pageable) {
        return ResponseEntity.ok(orderService.getBuyerOrders(buyerId, pageable));
    }

    @GetMapping("/api/v1/sellers/{sellerId}/orders")
    @Operation(summary = "Orders containing this seller's items, most recent first")
    public ResponseEntity<Page<OrderDTOs.OrderListResponse>> bySeller(@PathVariable Long sellerId, Pageable pageable) {
        return ResponseEntity.ok(orderService.getSellerOrders(sellerId, pageable));
    }

    @PostMapping("/api/v1/sellers/{sellerId}/orders/{orderId}/pack")
    @Operation(summary = "Seller: mark a confirmed order as packed")
    public ResponseEntity<OrderDTOs.OrderResponse> pack(@PathVariable Long sellerId, @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.markPacked(orderId, sellerId));
    }

    @PostMapping("/api/v1/sellers/{sellerId}/orders/{orderId}/ship")
    @Operation(summary = "Seller: mark a packed order as shipped (Rule 5: within 48h)")
    public ResponseEntity<OrderDTOs.OrderResponse> ship(
            @PathVariable Long sellerId, @PathVariable Long orderId, @RequestParam String trackingNumber) {
        return ResponseEntity.ok(orderService.shipOrder(orderId, sellerId, trackingNumber));
    }

    @PostMapping("/api/v1/orders/{orderId}/deliver")
    @Operation(summary = "Confirm delivery of a shipped order")
    public ResponseEntity<OrderDTOs.OrderResponse> deliver(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.markDelivered(orderId));
    }

    @PostMapping("/api/v1/users/{buyerId}/orders/{orderId}/cancel")
    @Operation(summary = "Buyer: cancel an order before it ships (restocks automatically)")
    public ResponseEntity<OrderDTOs.OrderResponse> cancel(
            @PathVariable Long buyerId, @PathVariable Long orderId, @Valid @RequestBody OrderDTOs.CancelOrderRequest request) {
        return ResponseEntity.ok(orderService.cancelOrder(orderId, buyerId, request));
    }

    @PostMapping("/api/v1/users/{buyerId}/orders/{orderId}/return")
    @Operation(summary = "Buyer: start a return on a delivered item")
    public ResponseEntity<OrderDTOs.OrderResponse> initiateReturn(
            @PathVariable Long buyerId, @PathVariable Long orderId, @Valid @RequestBody OrderDTOs.InitiateReturnRequest request) {
        return ResponseEntity.ok(orderService.initiateReturn(orderId, buyerId, request));
    }
}
