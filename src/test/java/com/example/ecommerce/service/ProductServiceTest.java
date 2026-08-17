package com.example.ecommerce.service;

import com.example.ecommerce.exception.OutOfStockException;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.repository.CategoryRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.SellerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService")
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private SellerProfileRepository sellerProfileRepository;
    @Mock private CategoryRepository categoryRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, sellerProfileRepository, categoryRepository);
    }

    @Test
    @DisplayName("Rule 1: reduceStock uses the row-locking finder, not a plain findById")
    void reduceStockUsesLockingRead() {
        Product product = Product.builder().id(1L).stockQuantity(10).build();
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.reduceStock(1L, 4);

        assertThat(product.getStockQuantity()).isEqualTo(6);
    }

    @Test
    @DisplayName("Rule 1: stock quantity must never go below 0")
    void reduceStockRejectsOversell() {
        Product product = Product.builder().id(1L).stockQuantity(2).build();
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.reduceStock(1L, 5))
            .isInstanceOf(OutOfStockException.class);
        assertThat(product.getStockQuantity()).isEqualTo(2);
    }
}
