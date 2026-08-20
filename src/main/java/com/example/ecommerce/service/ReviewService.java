package com.example.ecommerce.service;

import com.example.ecommerce.dto.ReviewDTOs;
import com.example.ecommerce.exception.DuplicateResourceException;
import com.example.ecommerce.exception.InvalidOperationException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.model.OrderItem;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.model.Review;
import com.example.ecommerce.model.User;
import com.example.ecommerce.repository.OrderItemRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.ReviewRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Rule 4 (Review Eligibility): verified-purchase only, one review per product per buyer,
 * editable for 30 days then locked. Keeps products.averageRating/totalReviews in sync
 * (Decision 2 in the schema doc: denormalized for fast listing, recomputed here on write).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private static final int EDIT_WINDOW_DAYS = 30;

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ReviewDTOs.ReviewResponse createReview(Long buyerId, ReviewDTOs.CreateReviewRequest request) {
        User buyer = userRepository.findById(buyerId)
            .orElseThrow(() -> new ResourceNotFoundException("User", buyerId));
        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new ResourceNotFoundException("Product", request.productId()));

        if (reviewRepository.findByProductIdAndBuyerId(request.productId(), buyerId).isPresent()) {
            throw new DuplicateResourceException("Review already exists for this buyer and product");
        }

        List<OrderItem> delivered = orderItemRepository.findDeliveredByBuyerAndProduct(buyerId, request.productId());
        if (delivered.isEmpty()) {
            throw new InvalidOperationException("Only buyers with a delivered order for this product can review it");
        }

        Review review = Review.builder()
            .product(product)
            .buyer(buyer)
            .orderItem(delivered.get(0))
            .rating(request.rating())
            .reviewTitle(request.reviewTitle())
            .reviewComment(request.reviewComment())
            .verifiedPurchase(true)
            .helpfulCount(0)
            .reviewStatus(Review.ReviewStatus.PENDING)
            .isLocked(false)
            .build();

        Review saved = reviewRepository.save(review);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ReviewDTOs.ReviewListResponse> getProductReviews(Long productId, boolean mostHelpfulFirst, Pageable pageable) {
        Page<Review> reviews = mostHelpfulFirst
            ? reviewRepository.findByProductIdAndReviewStatusOrderByRatingDesc(productId, Review.ReviewStatus.APPROVED, pageable)
            : reviewRepository.findByProductIdAndReviewStatusOrderByCreatedAtDesc(productId, Review.ReviewStatus.APPROVED, pageable);
        return reviews.map(this::mapToListResponse);
    }

    @Transactional(readOnly = true)
    public Page<ReviewDTOs.ReviewResponse> getBuyerReviews(Long buyerId, Pageable pageable) {
        return reviewRepository.findByBuyerId(buyerId, pageable).map(this::mapToResponse);
    }

    /** Admin: reviews awaiting moderation (or any other status), across all products. */
    @Transactional(readOnly = true)
    public Page<ReviewDTOs.ReviewResponse> getReviewsByStatus(String status, Pageable pageable) {
        Review.ReviewStatus reviewStatus = Review.ReviewStatus.valueOf(status);
        return reviewRepository.findByReviewStatusOrderByCreatedAtDesc(reviewStatus, pageable).map(this::mapToResponse);
    }

    /** Seller: reviews across all of their products, so they can respond regardless of moderation status. */
    @Transactional(readOnly = true)
    public Page<ReviewDTOs.ReviewResponse> getSellerReviews(Long sellerId, Pageable pageable) {
        return reviewRepository.findBySellerId(sellerId, pageable).map(this::mapToResponse);
    }

    public ReviewDTOs.ReviewResponse updateReview(Long buyerId, Long reviewId, ReviewDTOs.UpdateReviewRequest request) {
        Review review = requireOwnedReview(reviewId, buyerId);

        if (isPastEditWindow(review)) {
            lockReview(review);
            throw new InvalidOperationException("Review is more than " + EDIT_WINDOW_DAYS + " days old and is now locked");
        }
        if (Boolean.TRUE.equals(review.getIsLocked())) {
            throw new InvalidOperationException("Review is locked and can no longer be edited");
        }

        review.setRating(request.rating());
        review.setReviewTitle(request.reviewTitle());
        review.setReviewComment(request.reviewComment());
        review.setReviewStatus(Review.ReviewStatus.PENDING); // an edited review is re-moderated

        Review saved = reviewRepository.save(review);
        recomputeProductRating(saved.getProduct().getId());
        return mapToResponse(saved);
    }

    public void deleteReview(Long buyerId, Long reviewId) {
        Review review = requireOwnedReview(reviewId, buyerId);
        review.setDeletedAt(LocalDateTime.now());
        review.setReviewStatus(Review.ReviewStatus.HIDDEN);
        reviewRepository.save(review);
        recomputeProductRating(review.getProduct().getId());
    }

    public ReviewDTOs.ReviewResponse respondAsSeller(Long sellerId, Long reviewId, ReviewDTOs.SellerResponseRequest request) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));

        if (!review.getProduct().getSeller().getId().equals(sellerId)) {
            throw new InvalidOperationException("Seller can only respond to reviews of their own products");
        }

        review.setSellerResponse(request.response());
        review.setSellerResponseAt(LocalDateTime.now());
        return mapToResponse(reviewRepository.save(review));
    }

    /** Admin: approve/reject/hide a review; APPROVED reviews are what buyers see and what feed averageRating. */
    public ReviewDTOs.ReviewResponse moderateReview(Long reviewId, String status) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));

        review.setReviewStatus(Review.ReviewStatus.valueOf(status));
        Review saved = reviewRepository.save(review);
        recomputeProductRating(saved.getProduct().getId());
        return mapToResponse(saved);
    }

    /** Nightly lock sweep - an edit attempt also lazily locks a review, this just catches the rest. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void lockExpiredReviews() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(EDIT_WINDOW_DAYS);
        Pageable batch = PageRequest.of(0, 500, Sort.by("createdAt"));
        Page<Review> candidates = reviewRepository.findRecentReviews(Review.ReviewStatus.APPROVED, LocalDateTime.MIN, batch);
        candidates.getContent().stream()
            .filter(r -> !Boolean.TRUE.equals(r.getIsLocked()))
            .filter(r -> r.getCreatedAt().isBefore(cutoff))
            .forEach(this::lockReview);
    }

    private void lockReview(Review review) {
        review.setIsLocked(true);
        review.setLockedAt(LocalDateTime.now());
        reviewRepository.save(review);
    }

    private boolean isPastEditWindow(Review review) {
        return review.getCreatedAt().plusDays(EDIT_WINDOW_DAYS).isBefore(LocalDateTime.now());
    }

    private Review requireOwnedReview(Long reviewId, Long buyerId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));
        if (!review.getBuyer().getId().equals(buyerId)) {
            throw new InvalidOperationException("Review does not belong to this buyer");
        }
        return review;
    }

    private void recomputeProductRating(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow();
        Page<Review> approved = reviewRepository.findByProductIdAndReviewStatusOrderByCreatedAtDesc(
            productId, Review.ReviewStatus.APPROVED, Pageable.unpaged());

        int count = approved.getNumberOfElements();
        BigDecimal average = count == 0
            ? BigDecimal.ZERO
            : approved.getContent().stream()
                .map(r -> BigDecimal.valueOf(r.getRating()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

        product.setAverageRating(average);
        product.setTotalReviews(count);
        productRepository.save(product);
    }

    private ReviewDTOs.ReviewResponse mapToResponse(Review review) {
        return new ReviewDTOs.ReviewResponse(
            review.getId(),
            review.getProduct().getId(),
            review.getProduct().getProductName(),
            review.getBuyer().getId(),
            review.getBuyer().getFullName(),
            review.getRating(),
            review.getReviewTitle(),
            review.getReviewComment(),
            review.getVerifiedPurchase(),
            review.getHelpfulCount(),
            review.getReviewStatus().toString(),
            review.getIsLocked(),
            review.getSellerResponse(),
            review.getSellerResponseAt() != null ? review.getSellerResponseAt().toString() : null,
            review.getCreatedAt().toString()
        );
    }

    private ReviewDTOs.ReviewListResponse mapToListResponse(Review review) {
        return new ReviewDTOs.ReviewListResponse(
            review.getId(),
            review.getRating(),
            review.getReviewTitle(),
            review.getReviewComment(),
            review.getVerifiedPurchase(),
            review.getHelpfulCount(),
            review.getBuyer().getFullName(),
            review.getCreatedAt().toString(),
            review.getSellerResponse(),
            review.getSellerResponseAt() != null ? review.getSellerResponseAt().toString() : null
        );
    }
}
