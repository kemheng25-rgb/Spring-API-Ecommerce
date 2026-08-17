package com.example.ecommerce.service;

import com.example.ecommerce.dto.DisputeDTOs;
import com.example.ecommerce.exception.DuplicateResourceException;
import com.example.ecommerce.exception.InvalidOperationException;
import com.example.ecommerce.exception.UnauthorizedException;
import com.example.ecommerce.model.*;
import com.example.ecommerce.repository.DisputeRepository;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DisputeService")
class DisputeServiceTest {

    @Mock private DisputeRepository disputeRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;

    private DisputeService disputeService;
    private User buyer;
    private SellerProfile sellerProfile;
    private User sellerUser;

    @BeforeEach
    void setUp() {
        disputeService = new DisputeService(disputeRepository, orderRepository, userRepository, auditLogService);
        buyer = User.builder().id(1L).fullName("Buyer").build();
        sellerUser = User.builder().id(2L).fullName("Seller").build();
        sellerProfile = SellerProfile.builder().id(3L).shopName("Shop").user(sellerUser).build();
    }

    private Order orderDeliveredOn(LocalDate deliveryDate) {
        OrderItem item = OrderItem.builder().id(100L).seller(sellerProfile).build();
        return Order.builder().id(10L).buyer(buyer).actualDeliveryDate(deliveryDate)
            .orderDate(LocalDateTime.now().minusDays(40))
            .items(Set.of(item)).build();
    }

    @Test
    @DisplayName("Rule 6: rejects a dispute filed more than 14 days after delivery")
    void rejectsPastResolutionWindow() {
        Order order = orderDeliveredOn(LocalDate.now().minusDays(20));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        DisputeDTOs.CreateDisputeRequest request = new DisputeDTOs.CreateDisputeRequest(
            10L, "DAMAGED", "The item arrived broken in two places", null);

        assertThatThrownBy(() -> disputeService.createDispute(1L, request))
            .isInstanceOf(InvalidOperationException.class)
            .hasMessageContaining("14 days");
    }

    @Test
    @DisplayName("Rule 6: a NOT_RECEIVED claim gets the longer 30-day non-delivery window")
    void nonDeliveryGetsLongerWindow() {
        OrderItem item = OrderItem.builder().id(100L).seller(sellerProfile).build();
        Order order = Order.builder().id(10L).buyer(buyer).actualDeliveryDate(null)
            .orderDate(LocalDateTime.now().minusDays(20)).items(Set.of(item)).build();

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(disputeRepository.findByOrderId(10L)).thenReturn(Optional.empty());
        when(disputeRepository.findByDisputeNumber(any())).thenReturn(Optional.empty());
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> {
            Dispute d = inv.getArgument(0);
            d.setId(1L);
            d.setCreatedAt(LocalDateTime.now());
            return d;
        });

        DisputeDTOs.CreateDisputeRequest request = new DisputeDTOs.CreateDisputeRequest(
            10L, "NOT_RECEIVED", "Tracking says delivered but nothing arrived", null);

        DisputeDTOs.DisputeResponse response = disputeService.createDispute(1L, request);
        assertThat(response.disputeStatus()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("rejects a second dispute against the same order")
    void rejectsDuplicateDispute() {
        Order order = orderDeliveredOn(LocalDate.now().minusDays(2));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(disputeRepository.findByOrderId(10L)).thenReturn(Optional.of(Dispute.builder().id(5L).build()));

        DisputeDTOs.CreateDisputeRequest request = new DisputeDTOs.CreateDisputeRequest(
            10L, "DAMAGED", "The item arrived broken in two places", null);

        assertThatThrownBy(() -> disputeService.createDispute(1L, request))
            .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("rejects a dispute from someone who is neither the buyer nor a seller on the order")
    void rejectsUnrelatedInitiator() {
        Order order = orderDeliveredOn(LocalDate.now().minusDays(2));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        DisputeDTOs.CreateDisputeRequest request = new DisputeDTOs.CreateDisputeRequest(
            10L, "DAMAGED", "The item arrived broken in two places", null);

        assertThatThrownBy(() -> disputeService.createDispute(999L, request))
            .isInstanceOf(UnauthorizedException.class);
    }
}
