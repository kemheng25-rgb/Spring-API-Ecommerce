package com.example.ecommerce.controller;

import com.example.ecommerce.dto.ReviewDTOs;
import com.example.ecommerce.service.ReviewService;
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
@Tag(name = "Reviews", description = "Rule 4: verified-purchase reviews, one per product per buyer, locked after 30 days")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/v1/users/{buyerId}/reviews")
    @Operation(summary = "Leave a review (requires a delivered order for the product)")
    public ResponseEntity<ReviewDTOs.ReviewResponse> create(
            @PathVariable Long buyerId, @Valid @RequestBody ReviewDTOs.CreateReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.createReview(buyerId, request));
    }

    @GetMapping("/api/v1/products/{productId}/reviews")
    @Operation(summary = "Browse a product's approved reviews")
    public ResponseEntity<Page<ReviewDTOs.ReviewListResponse>> byProduct(
            @PathVariable Long productId, @RequestParam(defaultValue = "false") boolean mostHelpfulFirst, Pageable pageable) {
        return ResponseEntity.ok(reviewService.getProductReviews(productId, mostHelpfulFirst, pageable));
    }

    @GetMapping("/api/v1/users/{buyerId}/reviews")
    @Operation(summary = "A buyer's own reviews")
    public ResponseEntity<Page<ReviewDTOs.ReviewResponse>> byBuyer(@PathVariable Long buyerId, Pageable pageable) {
        return ResponseEntity.ok(reviewService.getBuyerReviews(buyerId, pageable));
    }

    @PutMapping("/api/v1/users/{buyerId}/reviews/{reviewId}")
    @Operation(summary = "Edit a review (blocked once locked, 30 days after posting)")
    public ResponseEntity<ReviewDTOs.ReviewResponse> update(
            @PathVariable Long buyerId, @PathVariable Long reviewId, @Valid @RequestBody ReviewDTOs.UpdateReviewRequest request) {
        return ResponseEntity.ok(reviewService.updateReview(buyerId, reviewId, request));
    }

    @DeleteMapping("/api/v1/users/{buyerId}/reviews/{reviewId}")
    @Operation(summary = "Hide a review")
    public ResponseEntity<Void> delete(@PathVariable Long buyerId, @PathVariable Long reviewId) {
        reviewService.deleteReview(buyerId, reviewId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/reviews")
    @Operation(summary = "Admin: list reviews by status (e.g. PENDING) for moderation, across all products")
    public ResponseEntity<Page<ReviewDTOs.ReviewResponse>> byStatus(@RequestParam String status, Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviewsByStatus(status, pageable));
    }

    @GetMapping("/api/v1/sellers/{sellerId}/reviews")
    @Operation(summary = "Seller: reviews across all of their products, so they can respond")
    public ResponseEntity<Page<ReviewDTOs.ReviewResponse>> bySeller(@PathVariable Long sellerId, Pageable pageable) {
        return ResponseEntity.ok(reviewService.getSellerReviews(sellerId, pageable));
    }

    @PostMapping("/api/v1/sellers/{sellerId}/reviews/{reviewId}/response")
    @Operation(summary = "Seller responds to a review of their product")
    public ResponseEntity<ReviewDTOs.ReviewResponse> respond(
            @PathVariable Long sellerId, @PathVariable Long reviewId, @Valid @RequestBody ReviewDTOs.SellerResponseRequest request) {
        return ResponseEntity.ok(reviewService.respondAsSeller(sellerId, reviewId, request));
    }

    @PostMapping("/api/v1/reviews/{reviewId}/moderate")
    @Operation(summary = "Admin: approve, reject or hide a review")
    public ResponseEntity<ReviewDTOs.ReviewResponse> moderate(@PathVariable Long reviewId, @RequestParam String status) {
        return ResponseEntity.ok(reviewService.moderateReview(reviewId, status));
    }
}
