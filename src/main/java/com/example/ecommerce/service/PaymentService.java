package com.example.ecommerce.service;

import com.example.ecommerce.dto.PaymentDTOs;
import com.example.ecommerce.event.PaymentCompletedEvent;
import com.example.ecommerce.exception.InvalidOperationException;
import com.example.ecommerce.exception.PaymentFailedException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.model.Order;
import com.example.ecommerce.model.Payment;
import com.example.ecommerce.model.User;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.PaymentRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Rule 3 (Payment Completeness): an order is only ever CONFIRMED by a completed payment here -
 * OrderService never confirms itself. Stock is reduced once, at order placement (Rule 1); a
 * failed attempt leaves the order PENDING so the buyer can retry payment or cancel for a
 * full restock - it is not re-reduced or restored per attempt.
 *
 * There is no real payment gateway wired up. `paymentGatewayToken` is treated as a stand-in for
 * a gateway's client-side token: blank/missing means the simulated gateway declines the charge.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;
    private final SellerLedgerService sellerLedgerService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public PaymentDTOs.PaymentResponse processPayment(Long buyerId, PaymentDTOs.ProcessPaymentRequest request) {
        User buyer = userRepository.findById(buyerId)
            .orElseThrow(() -> new ResourceNotFoundException("User", buyerId));

        Order order = orderRepository.findById(request.orderId())
            .orElseThrow(() -> new ResourceNotFoundException("Order", request.orderId()));

        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new InvalidOperationException("Order does not belong to this buyer");
        }
        if (order.getOrderStatus() != Order.OrderStatus.PENDING) {
            throw new InvalidOperationException("Order is not awaiting payment; its status is " + order.getOrderStatus());
        }
        if (request.amount().compareTo(order.getTotalAmount()) != 0) {
            throw new InvalidOperationException(
                "Payment amount " + request.amount() + " does not match order total " + order.getTotalAmount());
        }

        Payment payment = Payment.builder()
            .order(order)
            .buyer(buyer)
            .amount(request.amount())
            .currency("USD")
            .paymentMethod(Payment.PaymentMethod.valueOf(request.paymentMethod()))
            .paymentStatus(Payment.PaymentStatus.PENDING)
            .paymentGateway("SIMULATED")
            .refundStatus(Payment.RefundStatus.NOT_REFUNDED)
            .totalRefundedAmount(BigDecimal.ZERO)
            .build();

        boolean gatewayApproved = request.paymentGatewayToken() != null && !request.paymentGatewayToken().isBlank();

        if (!gatewayApproved) {
            payment.setPaymentStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new PaymentFailedException(null, "Gateway declined the charge (no valid payment token)");
        }

        payment.setPaymentStatus(Payment.PaymentStatus.COMPLETED);
        payment.setTransactionId("TXN-" + UUID.randomUUID());
        payment.setPaymentDate(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);

        orderService.confirmOrder(order.getId());
        sellerLedgerService.recordSale(order);
        applicationEventPublisher.publishEvent(
            new PaymentCompletedEvent(saved.getId(), order.getId(), buyerId, saved.getAmount(), saved.getTransactionId()));

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public PaymentDTOs.PaymentResponse getPayment(Long paymentId) {
        return mapToResponse(requirePayment(paymentId));
    }

    @Transactional(readOnly = true)
    public PaymentDTOs.PaymentResponse getPaymentByOrder(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order", orderId));
        return mapToResponse(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentDTOs.PaymentResponse> getBuyerPayments(Long buyerId, Pageable pageable) {
        return paymentRepository.findByBuyerId(buyerId, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<PaymentDTOs.PaymentResponse> getByStatus(String status, Pageable pageable) {
        return paymentRepository.findByPaymentStatusOrderByPaymentDateDesc(Payment.PaymentStatus.valueOf(status), pageable)
            .map(this::mapToResponse);
    }

    Payment requirePayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
    }

    private PaymentDTOs.PaymentResponse mapToResponse(Payment payment) {
        return new PaymentDTOs.PaymentResponse(
            payment.getId(),
            payment.getOrder().getId(),
            payment.getAmount(),
            payment.getPaymentMethod().toString(),
            payment.getPaymentStatus().toString(),
            payment.getPaymentDate() != null ? payment.getPaymentDate().toString() : null,
            payment.getRefundStatus().toString(),
            payment.getTotalRefundedAmount()
        );
    }
}
