package com.example.ecommerce.service;

import com.example.ecommerce.dto.ProductDTOs;
import com.example.ecommerce.exception.OutOfStockException;
import com.example.ecommerce.model.Category;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.model.SellerProfile;
import com.example.ecommerce.repository.CategoryRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.SellerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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

    @Test
    @DisplayName("createProduct defaults a missing discountPercentage to zero instead of leaving it null")
    void createProductDefaultsMissingDiscountToZero() {
        SellerProfile seller = SellerProfile.builder().id(1L).shopName("Shop")
            .verificationStatus(SellerProfile.VerificationStatus.VERIFIED).build();
        Category category = Category.builder().id(2L).categoryName("Widgets").build();

        when(sellerProfileRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(productRepository.findBySku("SKU-1")).thenReturn(Optional.empty());
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductDTOs.ProductCreateRequest request = new ProductDTOs.ProductCreateRequest(
            "Widget", "A widget", "SKU-1", new BigDecimal("9.99"), 10, 2L, null);

        ProductDTOs.ProductResponse response = productService.createProduct(1L, request);

        assertThat(response.discountPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("updateProduct preserves the existing discountPercentage when the request omits it")
    void updateProductPreservesDiscountWhenOmitted() {
        SellerProfile seller = SellerProfile.builder().id(1L).shopName("Shop").build();
        Category category = Category.builder().id(2L).categoryName("Widgets").build();
        Product product = Product.builder().id(1L).seller(seller).category(category)
            .discountPercentage(new BigDecimal("15.00")).build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductDTOs.ProductUpdateRequest request = new ProductDTOs.ProductUpdateRequest(
            "Widget", "Updated description", new BigDecimal("12.99"), 5, null, null);

        ProductDTOs.ProductResponse response = productService.updateProduct(1L, 1L, request);

        assertThat(response.discountPercentage()).isEqualByComparingTo("15.00");
    }
}
