package com.example.ecommerce.service;

import com.example.ecommerce.dto.OrderDTOs;
import com.example.ecommerce.exception.InvalidOperationException;
import com.example.ecommerce.exception.OutOfStockException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.model.*;
import com.example.ecommerce.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService")
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProductService productService;
    @Mock private OutboxEventRepository outboxEventRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, orderItemRepository, cartRepository,
            cartItemRepository, addressRepository, userRepository, productService,
            outboxEventRepository, new com.fasterxml.jackson.databind.ObjectMapper());
    }

    private User buyer(long id) {
        return User.builder().id(id).fullName("Buyer " + id).email("buyer" + id + "@e.com").build();
    }

    private SellerProfile seller(long id) {
        return SellerProfile.builder().id(id).shopName("Shop " + id).build();
    }

    private Product product(long id, SellerProfile sellerProfile, int stock) {
        return Product.builder().id(id).productName("Widget " + id).seller(sellerProfile)
            .price(new BigDecimal("19.99")).stockQuantity(stock)
            .productStatus(Product.ProductStatus.ACTIVE).build();
    }

    @Test
    @DisplayName("rejects placing an order from an empty cart")
    void placeOrderEmptyCart() {
        User buyer = buyer(1L);
        Cart cart = Cart.builder().id(10L).user(buyer).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(eq(10L), any())).thenReturn(org.springframework.data.domain.Page.empty());

        OrderDTOs.PlaceOrderRequest request = new OrderDTOs.PlaceOrderRequest(5L, null, "STANDARD");

        assertThatThrownBy(() -> orderService.placeOrder(1L, request))
            .isInstanceOf(InvalidOperationException.class)
            .hasMessageContaining("empty cart");
    }

    @Test
    @DisplayName("Rule 2: freezes the cart's priceAtAdd as the order line's unitPrice, not the product's current price")
    void placeOrderFreezesCartPrice() {
        User buyer = buyer(1L);
        Cart cart = Cart.builder().id(10L).user(buyer).build();
        SellerProfile sellerProfile = seller(2L);
        Product product = product(100L, sellerProfile, 5);
        product.setPrice(new BigDecimal("29.99")); // price has since gone UP from what the cart froze

        CartItem cartItem = CartItem.builder().id(500L).cart(cart).product(product)
            .quantity(2).priceAtAdd(new BigDecimal("19.99")).build();

        Address address = Address.builder().id(5L).user(buyer).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(eq(10L), any()))
            .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(cartItem)));
        when(addressRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(address));
        when(orderRepository.findByOrderNumber(any())).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getId() == null) o.setId(900L);
            o.setItems(o.getItems() != null ? o.getItems() : Set.of());
            return o;
        });
        Order persistedOrder = Order.builder().id(900L).buyer(buyer).orderStatus(Order.OrderStatus.PENDING)
            .totalAmount(new BigDecimal("39.98")).orderDate(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .items(Set.of(OrderItem.builder().id(1L).product(product).seller(sellerProfile).quantity(2)
                .unitPrice(new BigDecimal("19.99")).subtotal(new BigDecimal("39.98"))
                .itemStatus(OrderItem.ItemStatus.PENDING).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build()))
            .build();
        when(orderRepository.findById(900L)).thenReturn(Optional.of(persistedOrder));

        OrderDTOs.PlaceOrderRequest request = new OrderDTOs.PlaceOrderRequest(5L, null, "STANDARD");
        OrderDTOs.OrderResponse response = orderService.placeOrder(1L, request);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).unitPrice()).isEqualByComparingTo("19.99");
        verify(productService).reduceStock(100L, 2);
        verify(cartItemRepository).deleteByCartId(10L);
    }

    @Test
    @DisplayName("Scenario 2: surfaces an out-of-stock error raised while reducing stock at checkout")
    void placeOrderOutOfStock() {
        User buyer = buyer(1L);
        Cart cart = Cart.builder().id(10L).user(buyer).build();
        SellerProfile sellerProfile = seller(2L);
        Product product = product(100L, sellerProfile, 1);
        CartItem cartItem = CartItem.builder().id(500L).cart(cart).product(product)
            .quantity(3).priceAtAdd(new BigDecimal("19.99")).build();
        Address address = Address.builder().id(5L).user(buyer).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(eq(10L), any()))
            .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(cartItem)));
        when(addressRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(address));
        when(orderRepository.findByOrderNumber(any())).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(900L);
            return o;
        });
        doThrow(new OutOfStockException(100L, 3, 1)).when(productService).reduceStock(100L, 3);

        OrderDTOs.PlaceOrderRequest request = new OrderDTOs.PlaceOrderRequest(5L, null, "STANDARD");

        assertThatThrownBy(() -> orderService.placeOrder(1L, request))
            .isInstanceOf(OutOfStockException.class);
        verify(cartItemRepository, never()).deleteByCartId(anyLong());
    }

    @Test
    @DisplayName("Rule 3: confirmOrder rejects an order that is not PENDING")
    void confirmOrderWrongStatus() {
        Order order = Order.builder().id(1L).orderStatus(Order.OrderStatus.CONFIRMED).items(Set.of()).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.confirmOrder(1L))
            .isInstanceOf(InvalidOperationException.class)
            .hasMessageContaining("PENDING");
    }

    @Test
    @DisplayName("cancelOrder restocks every item and blocks cancellation once shipped")
    void cancelOrderRestocks() {
        User buyer = buyer(1L);
        SellerProfile sellerProfile = seller(2L);
        Product product = product(100L, sellerProfile, 0);
        OrderItem item = OrderItem.builder().id(1L).product(product).seller(sellerProfile)
            .quantity(4).itemStatus(OrderItem.ItemStatus.CONFIRMED).build();
        Order order = Order.builder().id(1L).buyer(buyer).orderStatus(Order.OrderStatus.CONFIRMED)
            .orderDate(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .items(new java.util.HashSet<>(Set.of(item))).build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.cancelOrder(1L, 1L, new OrderDTOs.CancelOrderRequest("changed my mind"));

        verify(productService).increaseStock(100L, 4);
        assertThat(order.getOrderStatus()).isEqualTo(Order.OrderStatus.CANCELLED);

        order.setOrderStatus(Order.OrderStatus.SHIPPED);
        assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L, new OrderDTOs.CancelOrderRequest("too late")))
            .isInstanceOf(InvalidOperationException.class);
    }
}
