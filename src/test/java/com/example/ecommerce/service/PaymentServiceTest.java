package com.example.ecommerce.service;

import com.example.ecommerce.dto.PaymentDTOs;
import com.example.ecommerce.event.PaymentCompletedEvent;
import com.example.ecommerce.exception.InvalidOperationException;
import com.example.ecommerce.exception.PaymentFailedException;
import com.example.ecommerce.model.Order;
import com.example.ecommerce.model.Payment;
import com.example.ecommerce.model.User;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.PaymentRepository;
import com.example.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService")
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderService orderService;
    @Mock private ApplicationEventPublisher applicationEventPublisher;

    private PaymentService paymentService;

    private User buyer;
    private Order order;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, orderRepository, userRepository, orderService,
            applicationEventPublisher);
        buyer = User.builder().id(1L).fullName("Buyer").email("b@e.com").build();
        order = Order.builder().id(10L).buyer(buyer).orderStatus(Order.OrderStatus.PENDING)
            .totalAmount(new BigDecimal("50.00")).items(Set.of()).build();
    }

    @Test
    @DisplayName("Rule 3: a completed payment confirms the order")
    void successfulPaymentConfirmsOrder() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentDTOs.ProcessPaymentRequest request = new PaymentDTOs.ProcessPaymentRequest(
            10L, "CREDIT_CARD", new BigDecimal("50.00"), "tok_valid");

        PaymentDTOs.PaymentResponse response = paymentService.processPayment(1L, request);

        assertThat(response.paymentStatus()).isEqualTo("COMPLETED");
        verify(orderService).confirmOrder(10L);

        ArgumentCaptor<PaymentCompletedEvent> captor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(10L);
        assertThat(captor.getValue().buyerId()).isEqualTo(1L);
        assertThat(captor.getValue().amount()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("Scenario 1: a declined gateway token fails the payment and leaves the order PENDING")
    void declinedPaymentDoesNotConfirmOrder() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentDTOs.ProcessPaymentRequest request = new PaymentDTOs.ProcessPaymentRequest(
            10L, "CREDIT_CARD", new BigDecimal("50.00"), "");

        assertThatThrownBy(() -> paymentService.processPayment(1L, request))
            .isInstanceOf(PaymentFailedException.class);

        verify(orderService, never()).confirmOrder(anyLong());
        verifyNoInteractions(applicationEventPublisher);
        assertThat(order.getOrderStatus()).isEqualTo(Order.OrderStatus.PENDING);
    }

    @Test
    @DisplayName("rejects a payment amount that doesn't match the order total")
    void amountMismatchRejected() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        PaymentDTOs.ProcessPaymentRequest request = new PaymentDTOs.ProcessPaymentRequest(
            10L, "CREDIT_CARD", new BigDecimal("1.00"), "tok_valid");

        assertThatThrownBy(() -> paymentService.processPayment(1L, request))
            .isInstanceOf(InvalidOperationException.class)
            .hasMessageContaining("does not match");
        verifyNoInteractions(paymentRepository);
    }

    @Test
    @DisplayName("rejects paying for someone else's order")
    void wrongBuyerRejected() {
        User otherBuyer = User.builder().id(99L).build();
        when(userRepository.findById(99L)).thenReturn(Optional.of(otherBuyer));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        PaymentDTOs.ProcessPaymentRequest request = new PaymentDTOs.ProcessPaymentRequest(
            10L, "CREDIT_CARD", new BigDecimal("50.00"), "tok_valid");

        assertThatThrownBy(() -> paymentService.processPayment(99L, request))
            .isInstanceOf(InvalidOperationException.class)
            .hasMessageContaining("does not belong");
    }
}
