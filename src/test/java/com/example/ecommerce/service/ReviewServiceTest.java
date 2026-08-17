package com.example.ecommerce.service;

import com.example.ecommerce.dto.ReviewDTOs;
import com.example.ecommerce.exception.DuplicateResourceException;
import com.example.ecommerce.exception.InvalidOperationException;
import com.example.ecommerce.model.OrderItem;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.model.Review;
import com.example.ecommerce.model.User;
import com.example.ecommerce.repository.OrderItemRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.ReviewRepository;
import com.example.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService")
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;

    private ReviewService reviewService;
    private User buyer;
    private Product product;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(reviewRepository, orderItemRepository, productRepository, userRepository);
        buyer = User.builder().id(1L).fullName("Buyer").build();
        product = Product.builder().id(50L).productName("Widget").build();
    }

    @Test
    @DisplayName("Rule 4: rejects a review with no delivered purchase of the product")
    void rejectsWithoutDeliveredPurchase() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(productRepository.findById(50L)).thenReturn(Optional.of(product));
        when(reviewRepository.findByProductIdAndBuyerId(50L, 1L)).thenReturn(Optional.empty());
        when(orderItemRepository.findDeliveredByBuyerAndProduct(1L, 50L)).thenReturn(List.of());

        ReviewDTOs.CreateReviewRequest request = new ReviewDTOs.CreateReviewRequest(50L, 5, "Great", "Loved it, works well");

        assertThatThrownBy(() -> reviewService.createReview(1L, request))
            .isInstanceOf(InvalidOperationException.class)
            .hasMessageContaining("delivered");
    }

    @Test
    @DisplayName("Rule 4: rejects a second review from the same buyer for the same product")
    void rejectsDuplicateReview() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(productRepository.findById(50L)).thenReturn(Optional.of(product));
        when(reviewRepository.findByProductIdAndBuyerId(50L, 1L))
            .thenReturn(Optional.of(Review.builder().id(9L).build()));

        ReviewDTOs.CreateReviewRequest request = new ReviewDTOs.CreateReviewRequest(50L, 4, "Good", "Solid product overall");

        assertThatThrownBy(() -> reviewService.createReview(1L, request))
            .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Rule 4: a review older than 30 days is locked and can no longer be edited")
    void locksReviewPastEditWindow() {
        Review oldReview = Review.builder().id(9L).buyer(buyer).product(product)
            .rating(3).isLocked(false)
            .createdAt(LocalDateTime.now().minusDays(31))
            .build();
        when(reviewRepository.findById(9L)).thenReturn(Optional.of(oldReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewDTOs.UpdateReviewRequest request = new ReviewDTOs.UpdateReviewRequest(5, "Updated", "Changed my mind about this");

        assertThatThrownBy(() -> reviewService.updateReview(1L, 9L, request))
            .isInstanceOf(InvalidOperationException.class)
            .hasMessageContaining("locked");
        assertThat(oldReview.getIsLocked()).isTrue();
    }

    @Test
    @DisplayName("accepts a review backed by a delivered order item")
    void acceptsEligibleReview() {
        OrderItem delivered = OrderItem.builder().id(200L).itemStatus(OrderItem.ItemStatus.DELIVERED).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(productRepository.findById(50L)).thenReturn(Optional.of(product));
        when(reviewRepository.findByProductIdAndBuyerId(50L, 1L)).thenReturn(Optional.empty());
        when(orderItemRepository.findDeliveredByBuyerAndProduct(1L, 50L)).thenReturn(List.of(delivered));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(1L);
            r.setCreatedAt(LocalDateTime.now());
            return r;
        });

        ReviewDTOs.CreateReviewRequest request = new ReviewDTOs.CreateReviewRequest(50L, 5, "Great", "Loved it, works well");
        ReviewDTOs.ReviewResponse response = reviewService.createReview(1L, request);

        assertThat(response.verifiedPurchase()).isTrue();
        assertThat(response.reviewStatus()).isEqualTo("PENDING");
    }
}
