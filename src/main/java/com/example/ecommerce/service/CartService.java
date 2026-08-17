package com.example.ecommerce.service;

import com.example.ecommerce.dto.CartDTOs;
import com.example.ecommerce.exception.InvalidOperationException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.model.Cart;
import com.example.ecommerce.model.CartItem;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.model.User;
import com.example.ecommerce.repository.CartItemRepository;
import com.example.ecommerce.repository.CartRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    /** "Note: Cart expires after 30 days of inactivity" (Phase 1 data requirements). */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void clearExpiredCarts() {
        List<Cart> expired = cartRepository.findByExpiresAtBefore(LocalDateTime.now());
        expired.forEach(cart -> cartItemRepository.deleteByCartId(cart.getId()));
    }
    
    public Cart getOrCreateCart(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        return cartRepository.findByUserId(userId)
            .orElseGet(() -> {
                Cart cart = Cart.builder()
                    .user(user)
                    .build();
                return cartRepository.save(cart);
            });
    }
    
    public CartDTOs.CartResponse addToCart(Long userId, CartDTOs.AddToCartRequest request) {
        Cart cart = getOrCreateCart(userId);
        
        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new ResourceNotFoundException("Product", request.productId()));
        
        if (product.getProductStatus() != Product.ProductStatus.ACTIVE) {
            throw new InvalidOperationException("Product is not available for purchase");
        }
        
        if (product.getStockQuantity() < request.quantity()) {
            throw new InvalidOperationException("Not enough stock available. Available: " + product.getStockQuantity());
        }
        
        // Check if product already in cart
        CartItem existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
            .orElse(null);
        
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + request.quantity());
            cartItemRepository.save(existingItem);
        } else {
            CartItem cartItem = CartItem.builder()
                .cart(cart)
                .product(product)
                .quantity(request.quantity())
                .priceAtAdd(product.getPrice())
                .build();
            cartItemRepository.save(cartItem);
        }
        
        return mapToCartResponse(cart);
    }
    
    public CartDTOs.CartResponse updateCartItem(Long userId, Long cartItemId, 
                                                CartDTOs.UpdateCartItemRequest request) {
        Cart cart = cartRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user", userId));
        
        CartItem cartItem = cartItemRepository.findByIdAndCartId(cartItemId, cart.getId())
            .orElseThrow(() -> new ResourceNotFoundException("CartItem", cartItemId));
        
        Product product = cartItem.getProduct();
        if (product.getStockQuantity() < request.quantity()) {
            throw new InvalidOperationException("Not enough stock available. Available: " + product.getStockQuantity());
        }
        
        cartItem.setQuantity(request.quantity());
        cartItemRepository.save(cartItem);
        
        return mapToCartResponse(cart);
    }
    
    public CartDTOs.CartResponse removeFromCart(Long userId, Long cartItemId) {
        Cart cart = cartRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user", userId));
        
        CartItem cartItem = cartItemRepository.findByIdAndCartId(cartItemId, cart.getId())
            .orElseThrow(() -> new ResourceNotFoundException("CartItem", cartItemId));
        
        cartItemRepository.delete(cartItem);
        return mapToCartResponse(cart);
    }
    
    public CartDTOs.CartResponse getCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user", userId));
        
        return mapToCartResponse(cart);
    }
    
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user", userId));
        
        cartItemRepository.deleteByCartId(cart.getId());
    }
    
    private CartDTOs.CartResponse mapToCartResponse(Cart cart) {
        BigDecimal totalAmount = cart.getItems().stream()
            .map(item -> item.getPriceAtAdd().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return new CartDTOs.CartResponse(
            cart.getId(),
            cart.getUser().getId(),
            cart.getItems().stream()
                .map(item -> new CartDTOs.CartItemResponse(
                    item.getId(),
                    item.getProduct().getId(),
                    item.getProduct().getProductName(),
                    item.getProduct().getProductStatus().toString(),
                    item.getQuantity(),
                    item.getPriceAtAdd(),
                    item.getPriceAtAdd().multiply(BigDecimal.valueOf(item.getQuantity()))
                ))
                .collect(Collectors.toList()),
            totalAmount,
            cart.getItems().size(),
            cart.getExpiresAt().toString()
        );
    }
}
