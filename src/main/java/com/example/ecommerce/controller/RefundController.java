package com.example.ecommerce.controller;

import com.example.ecommerce.dto.PaymentDTOs;
import com.example.ecommerce.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Refunds", description = "Rule 7: total refunds can never exceed the original payment")
public class RefundController {

    private final RefundService refundService;

    @PostMapping("/users/{initiatedByUserId}/refunds")
    @Operation(summary = "Issue a refund (buyer, seller, or admin) against a completed payment")
    public ResponseEntity<PaymentDTOs.RefundResponse> processRefund(
            @PathVariable Long initiatedByUserId, @Valid @RequestBody PaymentDTOs.ProcessRefundRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(refundService.processRefund(initiatedByUserId, request));
    }

    @GetMapping("/refunds/{id}")
    @Operation(summary = "Get a refund")
    public ResponseEntity<PaymentDTOs.RefundResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(refundService.getRefund(id));
    }

    @GetMapping("/payments/{paymentId}/refunds")
    @Operation(summary = "All refunds issued against a payment")
    public ResponseEntity<List<PaymentDTOs.RefundResponse>> forPayment(@PathVariable Long paymentId) {
        return ResponseEntity.ok(refundService.getRefundsForPayment(paymentId));
    }

    @GetMapping("/refunds")
    @Operation(summary = "Admin: refunds by status")
    public ResponseEntity<Page<PaymentDTOs.RefundResponse>> byStatus(@RequestParam String status, Pageable pageable) {
        return ResponseEntity.ok(refundService.getByStatus(status, pageable));
    }
}
