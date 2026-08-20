package com.example.ecommerce.service;

import com.example.ecommerce.dto.SellerLedgerDTOs;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.model.Order;
import com.example.ecommerce.model.OrderItem;
import com.example.ecommerce.model.Payment;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.model.Refund;
import com.example.ecommerce.model.SellerLedgerEntry;
import com.example.ecommerce.model.SellerProfile;
import com.example.ecommerce.repository.OrderItemRepository;
import com.example.ecommerce.repository.PaymentRepository;
import com.example.ecommerce.repository.SellerLedgerEntryRepository;
import com.example.ecommerce.repository.SellerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SellerLedgerService")
class SellerLedgerServiceTest {

    @Mock private SellerProfileRepository sellerProfileRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private SellerLedgerEntryRepository sellerLedgerEntryRepository;

    private SellerLedgerService sellerLedgerService;
    private SellerProfile seller;

    @BeforeEach
    void setUp() {
        sellerLedgerService = new SellerLedgerService(
            sellerProfileRepository, orderItemRepository, paymentRepository, sellerLedgerEntryRepository);
        seller = SellerProfile.builder().id(7L).commissionRate(new BigDecimal("10.00")).build();
    }

    private Order orderWith(Long orderId, BigDecimal... itemSubtotals) {
        Order order = Order.builder().id(orderId).orderNumber("ORD-1").orderDate(LocalDateTime.now()).build();
        Set<OrderItem> items = new HashSet<>();
        long nextItemId = 1000L;
        for (BigDecimal subtotal : itemSubtotals) {
            // Distinct ids matter here: OrderItem's equals()/hashCode() are id-only, so two
            // transient (id == null) items would collide as "equal" and the HashSet would
            // silently drop the second one.
            items.add(OrderItem.builder().id(nextItemId++).order(order).seller(seller).subtotal(subtotal).build());
        }
        order.setItems(items);
        return order;
    }

    @Test
    @DisplayName("maps a SALE entry to gross/commission/net using the rate snapshotted on the entry")
    void mapsSaleEntry() {
        when(sellerProfileRepository.findById(7L)).thenReturn(Optional.of(seller));
        Order order = orderWith(1L, new BigDecimal("100.00"));
        Product product = Product.builder().id(3L).productName("Widget").build();
        OrderItem item = order.getItems().iterator().next();
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("50.00"));
        item.setItemStatus(OrderItem.ItemStatus.DELIVERED);

        SellerLedgerEntry entry = SellerLedgerEntry.builder().id(500L).seller(seller).orderItem(item)
            .entryType(SellerLedgerEntry.EntryType.SALE)
            .grossAmount(new BigDecimal("100.00")).commissionRate(new BigDecimal("10.00"))
            .commissionAmount(new BigDecimal("10.00")).netAmount(new BigDecimal("90.00"))
            .description("Sale - Order ORD-1").createdAt(LocalDateTime.now()).build();

        when(sellerLedgerEntryRepository.findBySellerIdOrderByCreatedAtDesc(eq(7L), any()))
            .thenReturn(new PageImpl<>(List.of(entry)));
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.empty());

        var page = sellerLedgerService.getLedger(7L, Pageable.unpaged());
        SellerLedgerDTOs.LedgerEntryResponse response = page.getContent().get(0);

        assertThat(response.entryType()).isEqualTo("SALE");
        assertThat(response.grossAmount()).isEqualByComparingTo("100.00");
        assertThat(response.commissionAmount()).isEqualByComparingTo("10.00");
        assertThat(response.netAmount()).isEqualByComparingTo("90.00");
    }

    @Test
    @DisplayName("recordSale writes one SALE entry per item, skipping an item that already has one")
    void recordSaleWritesEntriesIdempotently() {
        Order order = orderWith(1L, new BigDecimal("100.00"), new BigDecimal("50.00"));
        List<OrderItem> items = List.copyOf(order.getItems());
        items.forEach(i -> i.setItemStatus(OrderItem.ItemStatus.CONFIRMED));

        // Simulate the second item already having a SALE entry from a prior (idempotent) call.
        when(sellerLedgerEntryRepository.findByOrderItemIdAndEntryType(items.get(0).getId(), SellerLedgerEntry.EntryType.SALE))
            .thenReturn(List.of());
        when(sellerLedgerEntryRepository.findByOrderItemIdAndEntryType(items.get(1).getId(), SellerLedgerEntry.EntryType.SALE))
            .thenReturn(List.of(SellerLedgerEntry.builder().id(1L).build()));

        sellerLedgerService.recordSale(order);

        ArgumentCaptor<SellerLedgerEntry> captor = ArgumentCaptor.forClass(SellerLedgerEntry.class);
        verify(sellerLedgerEntryRepository, times(1)).save(captor.capture());
        SellerLedgerEntry saved = captor.getValue();
        assertThat(saved.getEntryType()).isEqualTo(SellerLedgerEntry.EntryType.SALE);
        assertThat(saved.getOrderItem().getId()).isEqualTo(items.get(0).getId());
        assertThat(saved.getCommissionRate()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("recordRefundAdjustment splits the refund by each item's share of the order subtotal")
    void recordRefundAdjustmentSplitsProportionally() {
        Order order = orderWith(3L, new BigDecimal("100.00"), new BigDecimal("300.00"));
        List<OrderItem> items = List.copyOf(order.getItems());

        for (OrderItem item : items) {
            when(sellerLedgerEntryRepository.findByOrderItemIdAndEntryType(item.getId(), SellerLedgerEntry.EntryType.SALE))
                .thenReturn(List.of(SellerLedgerEntry.builder().commissionRate(new BigDecimal("10.00")).build()));
        }

        Payment payment = Payment.builder().id(20L).order(order).build();
        Refund refund = Refund.builder().id(1L).refundAmount(new BigDecimal("40.00")).refundReason("Damaged").build();

        sellerLedgerService.recordRefundAdjustment(payment, refund);

        ArgumentCaptor<SellerLedgerEntry> captor = ArgumentCaptor.forClass(SellerLedgerEntry.class);
        verify(sellerLedgerEntryRepository, times(2)).save(captor.capture());

        // The 100.00 item is 25% of the 400.00 order subtotal, so it absorbs 25% of the 40.00 refund = 10.00.
        SellerLedgerEntry smallItemAdjustment = captor.getAllValues().stream()
            .filter(e -> e.getGrossAmount().abs().compareTo(new BigDecimal("10.00")) == 0)
            .findFirst().orElseThrow();
        assertThat(smallItemAdjustment.getEntryType()).isEqualTo(SellerLedgerEntry.EntryType.REFUND_ADJUSTMENT);
        assertThat(smallItemAdjustment.getGrossAmount()).isEqualByComparingTo("-10.00");
        assertThat(smallItemAdjustment.getCommissionAmount()).isEqualByComparingTo("-1.00");
        assertThat(smallItemAdjustment.getNetAmount()).isEqualByComparingTo("-9.00");
    }

    @Test
    @DisplayName("recordCancellation fully reverses an item's existing SALE entry")
    void recordCancellationReversesSale() {
        Order order = orderWith(4L, new BigDecimal("100.00"));
        OrderItem item = order.getItems().iterator().next();
        SellerLedgerEntry sale = SellerLedgerEntry.builder().id(10L).seller(seller).orderItem(item)
            .entryType(SellerLedgerEntry.EntryType.SALE)
            .grossAmount(new BigDecimal("100.00")).commissionRate(new BigDecimal("10.00"))
            .commissionAmount(new BigDecimal("10.00")).netAmount(new BigDecimal("90.00")).build();
        when(sellerLedgerEntryRepository.findByOrderItemIdAndEntryType(item.getId(), SellerLedgerEntry.EntryType.SALE))
            .thenReturn(List.of(sale));

        sellerLedgerService.recordCancellation(order);

        ArgumentCaptor<SellerLedgerEntry> captor = ArgumentCaptor.forClass(SellerLedgerEntry.class);
        verify(sellerLedgerEntryRepository).save(captor.capture());
        SellerLedgerEntry reversal = captor.getValue();
        assertThat(reversal.getEntryType()).isEqualTo(SellerLedgerEntry.EntryType.CANCELLATION);
        assertThat(reversal.getGrossAmount()).isEqualByComparingTo("-100.00");
        assertThat(reversal.getNetAmount()).isEqualByComparingTo("-90.00");
    }

    @Test
    @DisplayName("recordCancellation is a no-op for an item with no SALE entry")
    void recordCancellationSkipsItemWithoutSale() {
        Order order = orderWith(5L, new BigDecimal("100.00"));
        OrderItem item = order.getItems().iterator().next();
        when(sellerLedgerEntryRepository.findByOrderItemIdAndEntryType(item.getId(), SellerLedgerEntry.EntryType.SALE))
            .thenReturn(List.of());

        sellerLedgerService.recordCancellation(order);

        verify(sellerLedgerEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("summary sums entries by the order item's current status bucket")
    void summarySumsEntriesByStatusBucket() {
        when(sellerProfileRepository.findById(7L)).thenReturn(Optional.of(seller));
        when(sellerLedgerEntryRepository.sumGrossBySellerIdAndItemStatus(7L, OrderItem.ItemStatus.DELIVERED))
            .thenReturn(new BigDecimal("200.00"));
        when(sellerLedgerEntryRepository.sumNetBySellerIdAndItemStatus(7L, OrderItem.ItemStatus.DELIVERED))
            .thenReturn(new BigDecimal("180.00"));
        when(sellerLedgerEntryRepository.sumGrossBySellerIdAndItemStatusIn(eq(7L), any()))
            .thenReturn(new BigDecimal("50.00"));
        when(sellerLedgerEntryRepository.sumNetBySellerIdAndItemStatusIn(eq(7L), any()))
            .thenReturn(new BigDecimal("45.00"));
        when(orderItemRepository.sumQuantityBySellerIdAndItemStatus(7L, OrderItem.ItemStatus.DELIVERED))
            .thenReturn(4L);

        SellerLedgerDTOs.LedgerSummaryResponse summary = sellerLedgerService.getSummary(7L);

        assertThat(summary.unitsSold()).isEqualTo(4L);
        assertThat(summary.availableGross()).isEqualByComparingTo("200.00");
        assertThat(summary.availableNet()).isEqualByComparingTo("180.00");
        assertThat(summary.pendingGross()).isEqualByComparingTo("50.00");
        assertThat(summary.pendingNet()).isEqualByComparingTo("45.00");
    }

    @Test
    @DisplayName("rejects an unknown seller")
    void rejectsUnknownSeller() {
        when(sellerProfileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sellerLedgerService.getSummary(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
