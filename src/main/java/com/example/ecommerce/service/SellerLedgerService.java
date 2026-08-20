package com.example.ecommerce.service;

import com.example.ecommerce.dto.SellerLedgerDTOs;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.model.Order;
import com.example.ecommerce.model.OrderItem;
import com.example.ecommerce.model.Payment;
import com.example.ecommerce.model.Refund;
import com.example.ecommerce.model.SellerLedgerEntry;
import com.example.ecommerce.model.SellerProfile;
import com.example.ecommerce.repository.OrderItemRepository;
import com.example.ecommerce.repository.PaymentRepository;
import com.example.ecommerce.repository.SellerLedgerEntryRepository;
import com.example.ecommerce.repository.SellerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Seller earnings ledger, backed by the append-only {@code seller_ledger_entries} table -
 * every financial event (a sale, a refund, a post-payment cancellation) writes a new row in
 * the same transaction as the event that caused it, and no row is ever updated afterwards.
 * That gives two things a fully-derived ledger couldn't: an audit trail (you can see a SALE
 * and the REFUND_ADJUSTMENT that reduced it as two separate history entries, not one
 * collapsed number) and immutability against a later commissionRate change (each row snapshots
 * the rate that was in effect when it was written, in `commissionRate`).
 *
 * Write paths (all in the same DB transaction as the triggering event, mirroring the outbox
 * principle in ARCHITECTURE.md - a financial record must never disagree with the event that
 * produced it):
 * - {@link #recordSale} - called by PaymentService.processPayment() right after the order is
 *   confirmed; one SALE entry per order item.
 * - {@link #recordRefundAdjustment} - called by RefundService.processRefund(); splits the
 *   refund across the order's items by each item's share of the order subtotal (a refund is
 *   recorded against the whole Payment, not per item, so this is a proportional estimate, not
 *   exact accounting), reusing the commissionRate from that item's original SALE entry so a
 *   later commissionRate change on the seller doesn't distort the reversal.
 * - {@link #recordCancellation} - called by OrderService.cancelOrder(); only for items that
 *   already have a SALE entry (i.e. the order had reached CONFIRMED before being cancelled),
 *   fully reversing that entry.
 *
 * Read paths ({@link #getLedger}, {@link #getSummary}) sum these entries; "available" vs
 * "pending" bucketing still comes from the order item's *current* itemStatus (delivered vs
 * still in flight), since that's a live fulfillment fact, not a financial one.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerLedgerService {

    private final SellerProfileRepository sellerProfileRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final SellerLedgerEntryRepository sellerLedgerEntryRepository;

    private static final List<OrderItem.ItemStatus> PENDING_BUCKET_STATUSES =
        List.of(OrderItem.ItemStatus.CONFIRMED, OrderItem.ItemStatus.PACKED, OrderItem.ItemStatus.SHIPPED);

    public Page<SellerLedgerDTOs.LedgerEntryResponse> getLedger(Long sellerId, Pageable pageable) {
        requireSeller(sellerId);
        return sellerLedgerEntryRepository.findBySellerIdOrderByCreatedAtDesc(sellerId, pageable)
            .map(this::mapToEntry);
    }

    public SellerLedgerDTOs.LedgerSummaryResponse getSummary(Long sellerId) {
        SellerProfile seller = requireSeller(sellerId);

        BigDecimal availableGross = sellerLedgerEntryRepository.sumGrossBySellerIdAndItemStatus(sellerId, OrderItem.ItemStatus.DELIVERED);
        BigDecimal availableNet = sellerLedgerEntryRepository.sumNetBySellerIdAndItemStatus(sellerId, OrderItem.ItemStatus.DELIVERED);
        BigDecimal pendingGross = sellerLedgerEntryRepository.sumGrossBySellerIdAndItemStatusIn(sellerId, PENDING_BUCKET_STATUSES);
        BigDecimal pendingNet = sellerLedgerEntryRepository.sumNetBySellerIdAndItemStatusIn(sellerId, PENDING_BUCKET_STATUSES);
        Long unitsSold = orderItemRepository.sumQuantityBySellerIdAndItemStatus(sellerId, OrderItem.ItemStatus.DELIVERED);

        return new SellerLedgerDTOs.LedgerSummaryResponse(
            seller.getCommissionRate(), unitsSold, availableGross, availableNet, pendingGross, pendingNet);
    }

    /** Writes one SALE entry per item in {@code order}, snapshotting each item's seller's current commissionRate. */
    @Transactional
    public void recordSale(Order order) {
        for (OrderItem item : order.getItems()) {
            if (!sellerLedgerEntryRepository.findByOrderItemIdAndEntryType(item.getId(), SellerLedgerEntry.EntryType.SALE).isEmpty()) {
                continue;
            }
            BigDecimal rate = item.getSeller().getCommissionRate();
            BigDecimal gross = item.getSubtotal();
            BigDecimal commission = gross.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            sellerLedgerEntryRepository.save(SellerLedgerEntry.builder()
                .seller(item.getSeller())
                .orderItem(item)
                .entryType(SellerLedgerEntry.EntryType.SALE)
                .grossAmount(gross)
                .commissionRate(rate)
                .commissionAmount(commission)
                .netAmount(gross.subtract(commission))
                .description("Sale - Order " + order.getOrderNumber())
                .build());
        }
    }

    /**
     * Splits {@code refund}'s amount across {@code payment}'s order items by each item's share
     * of the order subtotal, writing one REFUND_ADJUSTMENT entry per item that already has a
     * SALE entry. Reuses that SALE entry's commissionRate so the reversal isn't distorted by a
     * commissionRate change made after the original sale.
     */
    @Transactional
    public void recordRefundAdjustment(Payment payment, Refund refund) {
        Order order = payment.getOrder();
        BigDecimal orderSubtotal = order.getItems().stream()
            .map(OrderItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (orderSubtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        for (OrderItem item : order.getItems()) {
            List<SellerLedgerEntry> saleEntries =
                sellerLedgerEntryRepository.findByOrderItemIdAndEntryType(item.getId(), SellerLedgerEntry.EntryType.SALE);
            if (saleEntries.isEmpty()) {
                continue;
            }
            BigDecimal rate = saleEntries.get(0).getCommissionRate();
            BigDecimal itemShare = item.getSubtotal().divide(orderSubtotal, 6, RoundingMode.HALF_UP);
            BigDecimal grossReversal = refund.getRefundAmount().multiply(itemShare).setScale(2, RoundingMode.HALF_UP);
            if (grossReversal.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            BigDecimal commissionReversal = grossReversal.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            sellerLedgerEntryRepository.save(SellerLedgerEntry.builder()
                .seller(item.getSeller())
                .orderItem(item)
                .entryType(SellerLedgerEntry.EntryType.REFUND_ADJUSTMENT)
                .grossAmount(grossReversal.negate())
                .commissionRate(rate)
                .commissionAmount(commissionReversal.negate())
                .netAmount(grossReversal.subtract(commissionReversal).negate())
                .description("Refund - " + refund.getRefundReason())
                .build());
        }
    }

    /** Fully reverses the SALE entry for each item in {@code order} that already has one. */
    @Transactional
    public void recordCancellation(Order order) {
        for (OrderItem item : order.getItems()) {
            List<SellerLedgerEntry> saleEntries =
                sellerLedgerEntryRepository.findByOrderItemIdAndEntryType(item.getId(), SellerLedgerEntry.EntryType.SALE);
            if (saleEntries.isEmpty()) {
                continue;
            }
            SellerLedgerEntry sale = saleEntries.get(0);
            sellerLedgerEntryRepository.save(SellerLedgerEntry.builder()
                .seller(item.getSeller())
                .orderItem(item)
                .entryType(SellerLedgerEntry.EntryType.CANCELLATION)
                .grossAmount(sale.getGrossAmount().negate())
                .commissionRate(sale.getCommissionRate())
                .commissionAmount(sale.getCommissionAmount().negate())
                .netAmount(sale.getNetAmount().negate())
                .description("Order " + order.getOrderNumber() + " cancelled after payment")
                .build());
        }
    }

    private SellerLedgerDTOs.LedgerEntryResponse mapToEntry(SellerLedgerEntry entry) {
        OrderItem item = entry.getOrderItem();
        Order order = item.getOrder();
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);

        return new SellerLedgerDTOs.LedgerEntryResponse(
            entry.getId(),
            entry.getEntryType().toString(),
            entry.getDescription(),
            item.getId(),
            order.getId(),
            order.getOrderNumber(),
            order.getOrderDate().toString(),
            item.getProduct().getId(),
            item.getProduct().getProductName(),
            item.getQuantity(),
            item.getUnitPrice(),
            entry.getGrossAmount(),
            entry.getCommissionRate(),
            entry.getCommissionAmount(),
            entry.getNetAmount(),
            item.getItemStatus().toString(),
            payment != null ? payment.getPaymentStatus().toString() : null,
            payment != null ? payment.getRefundStatus().toString() : null,
            entry.getCreatedAt().toString()
        );
    }

    private SellerProfile requireSeller(Long sellerId) {
        return sellerProfileRepository.findById(sellerId)
            .orElseThrow(() -> new ResourceNotFoundException("SellerProfile", sellerId));
    }
}
