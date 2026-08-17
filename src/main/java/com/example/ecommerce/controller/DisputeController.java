package com.example.ecommerce.controller;

import com.example.ecommerce.dto.DisputeDTOs;
import com.example.ecommerce.service.DisputeService;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Disputes", description = "Workflow 6: buyer/seller conflict resolution")
public class DisputeController {

    private final DisputeService disputeService;

    @PostMapping("/users/{initiatorUserId}/disputes")
    @Operation(summary = "Open a dispute against an order (Rule 6: within the resolution window)")
    public ResponseEntity<DisputeDTOs.DisputeResponse> create(
            @PathVariable Long initiatorUserId, @Valid @RequestBody DisputeDTOs.CreateDisputeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(disputeService.createDispute(initiatorUserId, request));
    }

    @GetMapping("/disputes/{id}")
    @Operation(summary = "Get a dispute")
    public ResponseEntity<DisputeDTOs.DisputeResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(disputeService.getDispute(id));
    }

    @GetMapping("/users/{buyerId}/disputes")
    @Operation(summary = "A buyer's disputes")
    public ResponseEntity<Page<DisputeDTOs.DisputeListResponse>> byBuyer(@PathVariable Long buyerId, Pageable pageable) {
        return ResponseEntity.ok(disputeService.getBuyerDisputes(buyerId, pageable));
    }

    @GetMapping("/sellers/{sellerId}/disputes")
    @Operation(summary = "Disputes filed against a seller")
    public ResponseEntity<Page<DisputeDTOs.DisputeListResponse>> bySeller(@PathVariable Long sellerId, Pageable pageable) {
        return ResponseEntity.ok(disputeService.getSellerDisputes(sellerId, pageable));
    }

    @GetMapping("/disputes")
    @Operation(summary = "Admin dashboard: open disputes needing review")
    public ResponseEntity<Page<DisputeDTOs.DisputeListResponse>> open(Pageable pageable) {
        return ResponseEntity.ok(disputeService.getOpenDisputes(pageable));
    }

    @PostMapping("/disputes/{id}/assign")
    @Operation(summary = "Admin: claim an open dispute for review")
    public ResponseEntity<DisputeDTOs.DisputeResponse> assign(@PathVariable Long id, @RequestParam Long adminUserId) {
        return ResponseEntity.ok(disputeService.assignToAdmin(id, adminUserId));
    }

    @PostMapping("/disputes/{id}/resolve")
    @Operation(summary = "Admin: resolve a dispute with a reason (final, per Rule 8)")
    public ResponseEntity<DisputeDTOs.DisputeResponse> resolve(
            @PathVariable Long id, @RequestParam Long adminUserId, @Valid @RequestBody DisputeDTOs.ResolveDisputeRequest request) {
        return ResponseEntity.ok(disputeService.resolveDispute(adminUserId, id, request));
    }
}
