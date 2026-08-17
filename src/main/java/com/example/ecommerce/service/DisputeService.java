package com.example.ecommerce.service;

import com.example.ecommerce.dto.DisputeDTOs;
import com.example.ecommerce.exception.DuplicateResourceException;
import com.example.ecommerce.exception.InvalidOperationException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.exception.UnauthorizedException;
import com.example.ecommerce.model.Dispute;
import com.example.ecommerce.model.Order;
import com.example.ecommerce.model.User;
import com.example.ecommerce.repository.DisputeRepository;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Rule 6 (Dispute Resolution Window): 14 days after delivery, 30 days for non-delivery claims.
 * Rule 8 (Role Separation): once RESOLVED, the decision is final - there is no user-facing
 * "reopen"; only an admin resolving a fresh dispute changes anything after that point.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DisputeService {

    private static final int DELIVERED_WINDOW_DAYS = 14;
    private static final int NON_DELIVERY_WINDOW_DAYS = 30;
    private static final int MAX_NUMBER_ATTEMPTS = 5;

    private final DisputeRepository disputeRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public DisputeDTOs.DisputeResponse createDispute(Long initiatorUserId, DisputeDTOs.CreateDisputeRequest request) {
        Order order = orderRepository.findById(request.orderId())
            .orElseThrow(() -> new ResourceNotFoundException("Order", request.orderId()));

        Dispute.InitiatedBy initiatedBy = resolveInitiator(order, initiatorUserId);

        if (disputeRepository.findByOrderId(order.getId()).isPresent()) {
            throw new DuplicateResourceException("A dispute already exists for order " + order.getId());
        }

        validateWithinWindow(order, request.disputeReason());

        var seller = order.getItems().stream().findFirst()
            .map(item -> item.getSeller())
            .orElseThrow(() -> new InvalidOperationException("Order has no items to dispute"));

        Dispute dispute = Dispute.builder()
            .disputeNumber(generateDisputeNumber())
            .order(order)
            .buyer(order.getBuyer())
            .seller(seller)
            .initiatedBy(initiatedBy)
            .disputeReason(request.disputeReason())
            .description(request.description())
            .evidenceUrls(request.evidenceUrls())
            .disputeStatus(Dispute.DisputeStatus.OPEN)
            .resolution(Dispute.Resolution.NONE)
            .build();

        return mapToResponse(disputeRepository.save(dispute));
    }

    @Transactional(readOnly = true)
    public DisputeDTOs.DisputeResponse getDispute(Long disputeId) {
        return mapToResponse(requireDispute(disputeId));
    }

    @Transactional(readOnly = true)
    public Page<DisputeDTOs.DisputeListResponse> getBuyerDisputes(Long buyerId, Pageable pageable) {
        return disputeRepository.findByBuyerId(buyerId, pageable).map(this::mapToListResponse);
    }

    @Transactional(readOnly = true)
    public Page<DisputeDTOs.DisputeListResponse> getSellerDisputes(Long sellerId, Pageable pageable) {
        return disputeRepository.findBySellerId(sellerId, pageable).map(this::mapToListResponse);
    }

    /** Admin dashboard: "Find disputes needing review" (Phase 1 §7, query #9). */
    @Transactional(readOnly = true)
    public Page<DisputeDTOs.DisputeListResponse> getOpenDisputes(Pageable pageable) {
        return disputeRepository.findByDisputeStatusOrderByCreatedAtDesc(Dispute.DisputeStatus.OPEN, pageable)
            .map(this::mapToListResponse);
    }

    /** Admin: claim an open dispute for review. */
    public DisputeDTOs.DisputeResponse assignToAdmin(Long disputeId, Long adminUserId) {
        Dispute dispute = requireDispute(disputeId);
        User admin = userRepository.findById(adminUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User", adminUserId));

        dispute.setAssignedAdmin(admin);
        dispute.setDisputeStatus(Dispute.DisputeStatus.IN_REVIEW);
        return mapToResponse(disputeRepository.save(dispute));
    }

    /** Rule 8: admin must provide a reason; the decision is then final from the user's side. */
    public DisputeDTOs.DisputeResponse resolveDispute(Long adminUserId, Long disputeId, DisputeDTOs.ResolveDisputeRequest request) {
        Dispute dispute = requireDispute(disputeId);
        if (dispute.getDisputeStatus() == Dispute.DisputeStatus.RESOLVED || dispute.getDisputeStatus() == Dispute.DisputeStatus.CLOSED) {
            throw new InvalidOperationException("Dispute is already " + dispute.getDisputeStatus());
        }
        User admin = userRepository.findById(adminUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User", adminUserId));

        dispute.setAssignedAdmin(admin);
        dispute.setResolution(Dispute.Resolution.valueOf(request.resolution()));
        dispute.setAdminNotes(request.adminNotes());
        dispute.setDisputeStatus(Dispute.DisputeStatus.RESOLVED);
        dispute.setResolvedAt(LocalDateTime.now());

        Dispute saved = disputeRepository.save(dispute);
        auditLogService.log(adminUserId, "RESOLVE_DISPUTE", "DISPUTE", saved.getId(),
            Map.of("resolution", request.resolution(), "adminNotes", request.adminNotes()));
        return mapToResponse(saved);
    }

    private Dispute.InitiatedBy resolveInitiator(Order order, Long userId) {
        if (order.getBuyer().getId().equals(userId)) {
            return Dispute.InitiatedBy.BUYER;
        }
        boolean isSellerOnOrder = order.getItems().stream()
            .anyMatch(item -> item.getSeller().getUser().getId().equals(userId));
        if (isSellerOnOrder) {
            return Dispute.InitiatedBy.SELLER;
        }
        throw new UnauthorizedException("User " + userId + " is neither the buyer nor a seller on order " + order.getId());
    }

    private void validateWithinWindow(Order order, String disputeReason) {
        boolean nonDelivery = "NOT_RECEIVED".equalsIgnoreCase(disputeReason);
        if (nonDelivery) {
            if (order.getOrderDate().plusDays(NON_DELIVERY_WINDOW_DAYS).isBefore(LocalDateTime.now())) {
                throw new InvalidOperationException("Non-delivery disputes must be filed within " + NON_DELIVERY_WINDOW_DAYS + " days of the order");
            }
            return;
        }
        if (order.getActualDeliveryDate() == null) {
            throw new InvalidOperationException("This dispute reason requires a delivered order; use NOT_RECEIVED if it never arrived");
        }
        if (order.getActualDeliveryDate().plusDays(DELIVERED_WINDOW_DAYS).isBefore(LocalDateTime.now().toLocalDate())) {
            throw new InvalidOperationException("Disputes must be filed within " + DELIVERED_WINDOW_DAYS + " days of delivery");
        }
    }

    private Dispute requireDispute(Long disputeId) {
        return disputeRepository.findById(disputeId)
            .orElseThrow(() -> new ResourceNotFoundException("Dispute", disputeId));
    }

    private String generateDisputeNumber() {
        int year = Year.now().getValue();
        for (int attempt = 0; attempt < MAX_NUMBER_ATTEMPTS; attempt++) {
            String candidate = String.format("DSP-%d-%04d", year, ThreadLocalRandom.current().nextInt(10_000));
            if (disputeRepository.findByDisputeNumber(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new InvalidOperationException("Could not generate a unique dispute number, please retry");
    }

    private DisputeDTOs.DisputeResponse mapToResponse(Dispute dispute) {
        return new DisputeDTOs.DisputeResponse(
            dispute.getId(),
            dispute.getDisputeNumber(),
            dispute.getOrder().getId(),
            dispute.getOrder().getOrderNumber(),
            dispute.getInitiatedBy().toString(),
            dispute.getDisputeReason(),
            dispute.getDescription(),
            dispute.getDisputeStatus().toString(),
            dispute.getResolution().toString(),
            dispute.getAssignedAdmin() != null ? dispute.getAssignedAdmin().getFullName() : null,
            dispute.getAdminNotes(),
            dispute.getResolutionNotes(),
            dispute.getCreatedAt().toString(),
            dispute.getResolvedAt() != null ? dispute.getResolvedAt().toString() : null
        );
    }

    private DisputeDTOs.DisputeListResponse mapToListResponse(Dispute dispute) {
        return new DisputeDTOs.DisputeListResponse(
            dispute.getId(),
            dispute.getDisputeNumber(),
            dispute.getDisputeReason(),
            dispute.getDisputeStatus().toString(),
            dispute.getCreatedAt().toString(),
            dispute.getBuyer().getFullName(),
            dispute.getSeller().getShopName()
        );
    }
}
