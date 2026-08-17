package com.example.ecommerce.service;

import com.example.ecommerce.dto.OrderDTOs;
import com.example.ecommerce.event.OrderPlacedEvent;
import com.example.ecommerce.exception.InvalidOperationException;
import com.example.ecommerce.exception.OutOfStockException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.model.*;
import com.example.ecommerce.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Workflow 3 (Shopping &amp; Purchase) and Workflow 4 (Order Fulfillment) from Phase 1, plus
 * Rule 1 (inventory), Rule 2 (price locking) and Rule 3 (payment completeness). Payment
 * completeness is enforced by PaymentService calling {@link #confirmOrder}, not here - an order
 * created by this service always starts PENDING.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private static final int MAX_ORDER_NUMBER_ATTEMPTS = 5;
    private static final int ESTIMATED_SHIPPING_DAYS = 5;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final ProductService productService;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /** Rule 1 + Rule 2: reduce stock immediately, price is whatever was frozen in the cart. */
    public OrderDTOs.OrderResponse placeOrder(Long buyerId, OrderDTOs.PlaceOrderRequest request) {
        User buyer = userRepository.findById(buyerId)
            .orElseThrow(() -> new ResourceNotFoundException("User", buyerId));

        Cart cart = cartRepository.findByUserId(buyerId)
            .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user", buyerId));

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId(), Pageable.unpaged()).getContent();
        if (cartItems.isEmpty()) {
            throw new InvalidOperationException("Cannot place an order from an empty cart");
        }

        Address shippingAddress = addressRepository.findByIdAndUserId(request.shippingAddressId(), buyerId)
            .orElseThrow(() -> new ResourceNotFoundException("Address", request.shippingAddressId()));

        Address billingAddress = null;
        if (request.billingAddressId() != null) {
            billingAddress = addressRepository.findByIdAndUserId(request.billingAddressId(), buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", request.billingAddressId()));
        }

        Order order = Order.builder()
            .orderNumber(generateOrderNumber())
            .buyer(buyer)
            .orderStatus(Order.OrderStatus.PENDING)
            .totalAmount(BigDecimal.ZERO)
            .shippingAddress(shippingAddress)
            .billingAddress(billingAddress)
            .shippingMethod(request.shippingMethod())
            .estimatedDeliveryDate(LocalDateTime.now().plusDays(ESTIMATED_SHIPPING_DAYS).toLocalDate())
            .build();
        // Saved with an empty item set first, then items are persisted individually below and
        // re-fetched - deliberately never call order.getItems().add(...) here. Order/OrderItem
        // are Lombok @Data entities with a bidirectional link, so their equals/hashCode include
        // each other; adding a child to a live HashSet before both sides have stable identity
        // reshuffles every bucket in that set on each insert. Going through the repository avoids it.
        Order savedOrder = orderRepository.save(order);

        BigDecimal total = BigDecimal.ZERO;
        List<OrderPlacedEvent.Item> eventItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            // Scenario 2 (Phase 1): stock can have moved since the item was added to the cart -
            // re-check and reduce atomically here, not at add-to-cart time.
            try {
                productService.reduceStock(product.getId(), cartItem.getQuantity());
            } catch (OutOfStockException ex) {
                throw new OutOfStockException(
                    "'" + product.getProductName() + "' no longer has enough stock (" + ex.getMessage() + ")");
            }

            BigDecimal subtotal = cartItem.getPriceAtAdd().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            total = total.add(subtotal);

            OrderItem orderItem = OrderItem.builder()
                .order(savedOrder)
                .product(product)
                .seller(product.getSeller())
                .quantity(cartItem.getQuantity())
                .unitPrice(cartItem.getPriceAtAdd()) // frozen at cart-add time, never products.price
                .subtotal(subtotal)
                .itemStatus(OrderItem.ItemStatus.PENDING)
                .build();
            orderItemRepository.save(orderItem);
            eventItems.add(new OrderPlacedEvent.Item(product.getId(), product.getProductName(), cartItem.getQuantity()));
        }

        savedOrder.setTotalAmount(total);
        orderRepository.save(savedOrder);
        cartItemRepository.deleteByCartId(cart.getId());
        recordOrderPlacedEvent(savedOrder, buyerId, eventItems);

        return mapToResponse(requireOrder(savedOrder.getId()));
    }

    /** Same transaction as the order write above - see OutboxPublisherService for why. */
    private void recordOrderPlacedEvent(Order order, Long buyerId, List<OrderPlacedEvent.Item> items) {
        OrderPlacedEvent event = new OrderPlacedEvent(
            order.getId(), order.getOrderNumber(), buyerId, order.getTotalAmount(), items);
        try {
            outboxEventRepository.save(OutboxEvent.builder()
                .eventType("ORDER_PLACED")
                .aggregateId(order.getId())
                .payload(objectMapper.writeValueAsString(event))
                .status(OutboxEvent.Status.PENDING)
                .build());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize OrderPlacedEvent for order " + order.getId(), ex);
        }
    }

    @Transactional(readOnly = true)
    public OrderDTOs.OrderResponse getOrder(Long orderId) {
        return mapToResponse(requireOrder(orderId));
    }

    @Transactional(readOnly = true)
    public Page<OrderDTOs.OrderListResponse> getBuyerOrders(Long buyerId, Pageable pageable) {
        return orderRepository.findByBuyerIdOrderByOrderDateDesc(buyerId, pageable).map(this::mapToListResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderDTOs.OrderListResponse> getSellerOrders(Long sellerId, Pageable pageable) {
        return orderRepository.findBySellerIdOrderByOrderDateDesc(sellerId, pageable).map(this::mapToListResponse);
    }

    /** Rule 3: this is the ONLY path that moves an order out of PENDING - called by PaymentService. */
    public void confirmOrder(Long orderId) {
        Order order = requireOrder(orderId);
        if (order.getOrderStatus() != Order.OrderStatus.PENDING) {
            throw new InvalidOperationException("Order must be PENDING to confirm; it is " + order.getOrderStatus());
        }
        order.setOrderStatus(Order.OrderStatus.CONFIRMED);
        order.getItems().forEach(item -> item.setItemStatus(OrderItem.ItemStatus.CONFIRMED));
        orderRepository.save(order);
    }

    /** Rule 5: seller must ship within 48h of confirmation - packing is the first seller action. */
    public OrderDTOs.OrderResponse markPacked(Long orderId, Long sellerId) {
        Order order = requireSellerOrder(orderId, sellerId);
        requireStatus(order, Order.OrderStatus.CONFIRMED);
        order.setOrderStatus(Order.OrderStatus.PACKED);
        order.getItems().forEach(item -> item.setItemStatus(OrderItem.ItemStatus.PACKED));
        return mapToResponse(orderRepository.save(order));
    }

    public OrderDTOs.OrderResponse shipOrder(Long orderId, Long sellerId, String trackingNumber) {
        Order order = requireSellerOrder(orderId, sellerId);
        requireStatus(order, Order.OrderStatus.PACKED);
        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new InvalidOperationException("A tracking number is required to mark an order shipped");
        }
        order.setOrderStatus(Order.OrderStatus.SHIPPED);
        order.setTrackingNumber(trackingNumber);
        order.getItems().forEach(item -> item.setItemStatus(OrderItem.ItemStatus.SHIPPED));
        return mapToResponse(orderRepository.save(order));
    }

    /** Buyer (or an automated delivery webhook) confirms the parcel arrived. */
    public OrderDTOs.OrderResponse markDelivered(Long orderId) {
        Order order = requireOrder(orderId);
        requireStatus(order, Order.OrderStatus.SHIPPED);
        order.setOrderStatus(Order.OrderStatus.DELIVERED);
        order.setActualDeliveryDate(LocalDateTime.now().toLocalDate());
        order.getItems().forEach(item -> item.setItemStatus(OrderItem.ItemStatus.DELIVERED));
        return mapToResponse(orderRepository.save(order));
    }

    /**
     * Buyer cancellation, allowed only before the order ships. Restores stock immediately
     * (Rule 1). Does NOT trigger a refund automatically - if a payment was already completed,
     * call RefundService separately; keeping Order unaware of Payment avoids a circular
     * dependency between the two services.
     */
    public OrderDTOs.OrderResponse cancelOrder(Long orderId, Long buyerId, OrderDTOs.CancelOrderRequest request) {
        Order order = requireBuyerOrder(orderId, buyerId);
        if (order.getOrderStatus() != Order.OrderStatus.PENDING && order.getOrderStatus() != Order.OrderStatus.CONFIRMED) {
            throw new InvalidOperationException("Order can only be cancelled before it ships; current status is " + order.getOrderStatus());
        }

        order.getItems().forEach(item -> {
            productService.increaseStock(item.getProduct().getId(), item.getQuantity());
            item.setItemStatus(OrderItem.ItemStatus.CANCELLED);
        });
        order.setOrderStatus(Order.OrderStatus.CANCELLED);
        order.setNotes(appendNote(order.getNotes(), "Cancelled by buyer: " + request.reason()));

        return mapToResponse(orderRepository.save(order));
    }

    /** Workflow 4 step 6: buyer has a window after delivery to start a return on one item. */
    public OrderDTOs.OrderResponse initiateReturn(Long orderId, Long buyerId, OrderDTOs.InitiateReturnRequest request) {
        Order order = requireBuyerOrder(orderId, buyerId);
        requireStatus(order, Order.OrderStatus.DELIVERED);

        OrderItem item = order.getItems().stream()
            .filter(i -> i.getId().equals(request.orderItemId()))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("OrderItem", request.orderItemId()));

        item.setItemStatus(OrderItem.ItemStatus.RETURNED);
        orderItemRepository.save(item);

        if (order.getReturnInitiatedAt() == null) {
            order.setReturnInitiatedAt(LocalDateTime.now());
        }
        order.setNotes(appendNote(order.getNotes(), "Return initiated for item " + item.getId() + ": " + request.reason()));

        boolean allReturned = order.getItems().stream().allMatch(i -> i.getItemStatus() == OrderItem.ItemStatus.RETURNED);
        if (allReturned) {
            order.setOrderStatus(Order.OrderStatus.RETURNED);
        }

        return mapToResponse(orderRepository.save(order));
    }

    private Order requireOrder(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }

    private Order requireBuyerOrder(Long orderId, Long buyerId) {
        Order order = requireOrder(orderId);
        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new InvalidOperationException("Order does not belong to this buyer");
        }
        return order;
    }

    private Order requireSellerOrder(Long orderId, Long sellerId) {
        Order order = requireOrder(orderId);
        boolean ownsAnItem = order.getItems().stream().anyMatch(i -> i.getSeller().getId().equals(sellerId));
        if (!ownsAnItem) {
            throw new InvalidOperationException("Seller has no items on this order");
        }
        return order;
    }

    private void requireStatus(Order order, Order.OrderStatus expected) {
        if (order.getOrderStatus() != expected) {
            throw new InvalidOperationException("Expected order status " + expected + " but was " + order.getOrderStatus());
        }
    }

    private String appendNote(String existing, String addition) {
        return existing == null || existing.isBlank() ? addition : existing + " | " + addition;
    }

    private String generateOrderNumber() {
        int year = Year.now().getValue();
        for (int attempt = 0; attempt < MAX_ORDER_NUMBER_ATTEMPTS; attempt++) {
            String candidate = String.format("ORD-%d-%06d", year, ThreadLocalRandom.current().nextInt(1_000_000));
            if (orderRepository.findByOrderNumber(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new InvalidOperationException("Could not generate a unique order number, please retry");
    }

    private OrderDTOs.OrderResponse mapToResponse(Order order) {
        List<OrderDTOs.OrderItemResponse> items = order.getItems().stream()
            .map(item -> new OrderDTOs.OrderItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getProductName(),
                item.getSeller().getShopName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal(),
                item.getItemStatus().toString()
            ))
            .toList();

        return new OrderDTOs.OrderResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getOrderStatus().toString(),
            order.getTotalAmount(),
            order.getShippingMethod(),
            order.getTrackingNumber(),
            order.getEstimatedDeliveryDate() != null ? order.getEstimatedDeliveryDate().toString() : null,
            order.getActualDeliveryDate() != null ? order.getActualDeliveryDate().toString() : null,
            order.getOrderDate().toString(),
            order.getUpdatedAt().toString(),
            items
        );
    }

    private OrderDTOs.OrderListResponse mapToListResponse(Order order) {
        return new OrderDTOs.OrderListResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getOrderStatus().toString(),
            order.getTotalAmount(),
            order.getItems().size(),
            order.getOrderDate().toString()
        );
    }
}
