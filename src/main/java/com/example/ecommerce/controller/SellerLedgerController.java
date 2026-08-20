package com.example.ecommerce.controller;

import com.example.ecommerce.dto.SellerLedgerDTOs;
import com.example.ecommerce.service.SellerLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Seller Ledger", description = "Persisted, append-only seller earnings audit trail - see SellerLedgerService")
public class SellerLedgerController {

    private final SellerLedgerService sellerLedgerService;

    @GetMapping("/api/v1/sellers/{sellerId}/ledger")
    @Operation(summary = "Seller: paginated earnings history (SALE / REFUND_ADJUSTMENT / CANCELLATION entries)")
    public ResponseEntity<Page<SellerLedgerDTOs.LedgerEntryResponse>> getLedger(
            @PathVariable Long sellerId, Pageable pageable) {
        return ResponseEntity.ok(sellerLedgerService.getLedger(sellerId, pageable));
    }

    @GetMapping("/api/v1/sellers/{sellerId}/ledger/summary")
    @Operation(summary = "Seller: available (delivered) vs pending (in-flight) earnings totals")
    public ResponseEntity<SellerLedgerDTOs.LedgerSummaryResponse> getSummary(@PathVariable Long sellerId) {
        return ResponseEntity.ok(sellerLedgerService.getSummary(sellerId));
    }
}
