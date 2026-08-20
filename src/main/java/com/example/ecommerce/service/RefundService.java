package com.example.ecommerce.service;

import com.example.ecommerce.dto.PaymentDTOs;
import com.example.ecommerce.exception.InvalidOperationException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.model.Payment;
import com.example.ecommerce.model.Refund;
import com.example.ecommerce.model.User;
import com.example.ecommerce.repository.PaymentRepository;
import com.example.ecommerce.repository.RefundRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Rule 7 (Refund Integrity): total refunds against a payment can never exceed what was paid. */
@Service
@RequiredArgsConstructor
@Transactional
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final SellerLedgerService sellerLedgerService;

    public PaymentDTOs.RefundResponse processRefund(Long initiatedByUserId, PaymentDTOs.ProcessRefundRequest request) {
        User initiator = userRepository.findById(initiatedByUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User", initiatedByUserId));

        Payment payment = paymentRepository.findById(request.paymentId())
            .orElseThrow(() -> new ResourceNotFoundException("Payment", request.paymentId()));

        if (payment.getPaymentStatus() != Payment.PaymentStatus.COMPLETED) {
            throw new InvalidOperationException("Only a completed payment can be refunded; this one is " + payment.getPaymentStatus());
        }

        BigDecimal alreadyRefunded = refundRepository.findByPaymentId(payment.getId()).stream()
            .filter(r -> r.getRefundStatus() != Refund.RefundStatus.FAILED)
            .map(Refund::getRefundAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal newTotal = alreadyRefunded.add(request.refundAmount());
        if (newTotal.compareTo(payment.getAmount()) > 0) {
            throw new InvalidOperationException(String.format(
                "Refund of %s would exceed payment %s (already refunded %s)",
                request.refundAmount(), payment.getAmount(), alreadyRefunded));
        }

        Refund refund = Refund.builder()
            .payment(payment)
            .refundAmount(request.refundAmount())
            .refundReason(request.refundReason())
            .refundStatus(Refund.RefundStatus.COMPLETED)
            .refundTransactionId("RFD-" + UUID.randomUUID())
            .refundDate(LocalDateTime.now())
            .initiatedByUser(initiator)
            .notes(request.notes())
            .build();
        Refund saved = refundRepository.save(refund);

        payment.setTotalRefundedAmount(newTotal);
        payment.setRefundStatus(newTotal.compareTo(payment.getAmount()) == 0
            ? Payment.RefundStatus.FULLY_REFUNDED
            : Payment.RefundStatus.PARTIALLY_REFUNDED);
        if (payment.getRefundStatus() == Payment.RefundStatus.FULLY_REFUNDED) {
            payment.setPaymentStatus(Payment.PaymentStatus.REFUNDED);
        }
        paymentRepository.save(payment);
        sellerLedgerService.recordRefundAdjustment(payment, saved);

        auditLogService.log(initiatedByUserId, "PROCESS_REFUND", "PAYMENT", payment.getId(),
            Map.of("refundAmount", request.refundAmount().toString(), "refundReason", request.refundReason()));

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public PaymentDTOs.RefundResponse getRefund(Long refundId) {
        return mapToResponse(refundRepository.findById(refundId)
            .orElseThrow(() -> new ResourceNotFoundException("Refund", refundId)));
    }

    @Transactional(readOnly = true)
    public List<PaymentDTOs.RefundResponse> getRefundsForPayment(Long paymentId) {
        return refundRepository.findByPaymentId(paymentId).stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<PaymentDTOs.RefundResponse> getByStatus(String status, Pageable pageable) {
        return refundRepository.findByRefundStatusOrderByRefundDateDesc(Refund.RefundStatus.valueOf(status), pageable)
            .map(this::mapToResponse);
    }

    private PaymentDTOs.RefundResponse mapToResponse(Refund refund) {
        return new PaymentDTOs.RefundResponse(
            refund.getId(),
            refund.getPayment().getId(),
            refund.getRefundAmount(),
            refund.getRefundReason(),
            refund.getRefundStatus().toString(),
            refund.getRefundDate() != null ? refund.getRefundDate().toString() : null
        );
    }
}
