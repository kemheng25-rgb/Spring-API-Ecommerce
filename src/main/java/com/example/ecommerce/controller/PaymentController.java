package com.example.ecommerce.controller;

import com.example.ecommerce.dto.PaymentDTOs;
import com.example.ecommerce.service.PaymentService;
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
@Tag(name = "Payments", description = "Rule 3: an order is only confirmed by a completed payment")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/api/v1/users/{buyerId}/payments")
    @Operation(summary = "Attempt payment for a pending order")
    public ResponseEntity<PaymentDTOs.PaymentResponse> processPayment(
            @PathVariable Long buyerId, @Valid @RequestBody PaymentDTOs.ProcessPaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.processPayment(buyerId, request, idempotencyKey));
    }

    @GetMapping("/api/v1/payments/{id}")
    @Operation(summary = "Get a payment")
    public ResponseEntity<PaymentDTOs.PaymentResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPayment(id));
    }

    @GetMapping("/api/v1/orders/{orderId}/payment")
    @Operation(summary = "Get the (latest) payment for an order")
    public ResponseEntity<PaymentDTOs.PaymentResponse> byOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPaymentByOrder(orderId));
    }

    @GetMapping("/api/v1/users/{buyerId}/payments")
    @Operation(summary = "Buyer's payment history")
    public ResponseEntity<Page<PaymentDTOs.PaymentResponse>> byBuyer(@PathVariable Long buyerId, Pageable pageable) {
        return ResponseEntity.ok(paymentService.getBuyerPayments(buyerId, pageable));
    }

    @GetMapping("/api/v1/payments")
    @Operation(summary = "Admin: payments by status")
    public ResponseEntity<Page<PaymentDTOs.PaymentResponse>> byStatus(
            @RequestParam String status, Pageable pageable) {
        return ResponseEntity.ok(paymentService.getByStatus(status, pageable));
    }
}
