package com.example.ecommerce.service;

import com.example.ecommerce.dto.PaymentDTOs;
import com.example.ecommerce.exception.InvalidOperationException;
import com.example.ecommerce.model.Payment;
import com.example.ecommerce.model.Refund;
import com.example.ecommerce.model.User;
import com.example.ecommerce.repository.PaymentRepository;
import com.example.ecommerce.repository.RefundRepository;
import com.example.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefundService")
class RefundServiceTest {

    @Mock private RefundRepository refundRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private SellerLedgerService sellerLedgerService;

    private RefundService refundService;
    private User admin;
    private Payment payment;

    @BeforeEach
    void setUp() {
        refundService = new RefundService(refundRepository, paymentRepository, userRepository, auditLogService, sellerLedgerService);
        admin = User.builder().id(1L).fullName("Admin").build();
        payment = Payment.builder().id(20L).amount(new BigDecimal("100.00"))
            .paymentStatus(Payment.PaymentStatus.COMPLETED)
            .refundStatus(Payment.RefundStatus.NOT_REFUNDED)
            .totalRefundedAmount(BigDecimal.ZERO)
            .refunds(new java.util.HashSet<>())
            .build();
    }

    @Test
    @DisplayName("Rule 7: rejects a refund that would exceed what was actually paid")
    void rejectsOverRefund() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(paymentRepository.findById(20L)).thenReturn(Optional.of(payment));
        when(refundRepository.findByPaymentId(20L)).thenReturn(List.of(
            Refund.builder().refundAmount(new BigDecimal("80.00")).refundStatus(Refund.RefundStatus.COMPLETED).build()));

        PaymentDTOs.ProcessRefundRequest request = new PaymentDTOs.ProcessRefundRequest(
            20L, new BigDecimal("30.00"), "RETURN", null);

        assertThatThrownBy(() -> refundService.processRefund(1L, request))
            .isInstanceOf(InvalidOperationException.class)
            .hasMessageContaining("exceed");
    }

    @Test
    @DisplayName("a refund equal to the remaining balance marks the payment FULLY_REFUNDED")
    void fullRefundUpdatesPaymentStatus() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(paymentRepository.findById(20L)).thenReturn(Optional.of(payment));
        when(refundRepository.findByPaymentId(20L)).thenReturn(List.of());
        when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentDTOs.ProcessRefundRequest request = new PaymentDTOs.ProcessRefundRequest(
            20L, new BigDecimal("100.00"), "CANCELLATION", "full refund");

        refundService.processRefund(1L, request);

        assertThat(payment.getRefundStatus()).isEqualTo(Payment.RefundStatus.FULLY_REFUNDED);
        assertThat(payment.getPaymentStatus()).isEqualTo(Payment.PaymentStatus.REFUNDED);
        assertThat(payment.getTotalRefundedAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("rejects refunding a payment that never completed")
    void rejectsRefundOnIncompletePayment() {
        payment.setPaymentStatus(Payment.PaymentStatus.PENDING);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(paymentRepository.findById(20L)).thenReturn(Optional.of(payment));

        PaymentDTOs.ProcessRefundRequest request = new PaymentDTOs.ProcessRefundRequest(
            20L, new BigDecimal("10.00"), "RETURN", null);

        assertThatThrownBy(() -> refundService.processRefund(1L, request))
            .isInstanceOf(InvalidOperationException.class)
            .hasMessageContaining("completed payment");
    }
}
