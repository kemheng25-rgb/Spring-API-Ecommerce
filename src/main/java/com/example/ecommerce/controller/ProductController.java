package com.example.ecommerce.controller;

import com.example.ecommerce.dto.ProductDTOs;
import com.example.ecommerce.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Products", description = "Catalog browsing and seller listing management")
public class ProductController {

    private final ProductService productService;

    @PostMapping("/api/v1/sellers/{sellerId}/products")
    @Operation(summary = "List a new product (seller must be VERIFIED)")
    public ResponseEntity<ProductDTOs.ProductResponse> create(
            @PathVariable Long sellerId, @Valid @RequestBody ProductDTOs.ProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(sellerId, request));
    }

    @GetMapping("/api/v1/products/{id}")
    @Operation(summary = "Get a product by ID (increments its view count)")
    public ResponseEntity<ProductDTOs.ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @PutMapping("/api/v1/sellers/{sellerId}/products/{id}")
    @Operation(summary = "Update a product owned by this seller")
    public ResponseEntity<ProductDTOs.ProductResponse> update(
            @PathVariable Long sellerId, @PathVariable Long id, @Valid @RequestBody ProductDTOs.ProductUpdateRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, sellerId, request));
    }

    @GetMapping("/api/v1/categories/{categoryId}/products")
    @Operation(summary = "Browse active products in a category")
    public ResponseEntity<Page<ProductDTOs.ProductListResponse>> byCategory(@PathVariable Long categoryId, Pageable pageable) {
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId, pageable));
    }

    @GetMapping("/api/v1/sellers/{sellerId}/products")
    @Operation(summary = "Browse a seller's active listings")
    public ResponseEntity<Page<ProductDTOs.ProductListResponse>> bySeller(@PathVariable Long sellerId, Pageable pageable) {
        return ResponseEntity.ok(productService.getProductsBySeller(sellerId, pageable));
    }

    @GetMapping("/api/v1/products/featured")
    @Operation(summary = "Top-rated active products")
    public ResponseEntity<Page<ProductDTOs.ProductListResponse>> featured(Pageable pageable) {
        return ResponseEntity.ok(productService.getFeaturedProducts(pageable));
    }

    @GetMapping("/api/v1/products/discounted")
    @Operation(summary = "Active products with a live discount")
    public ResponseEntity<Page<ProductDTOs.ProductListResponse>> discounted(Pageable pageable) {
        return ResponseEntity.ok(productService.getDiscountedProducts(pageable));
    }

    @GetMapping("/api/v1/products/low-stock")
    @Operation(summary = "Seller/admin: active products at or below a stock threshold")
    public ResponseEntity<Page<ProductDTOs.ProductListResponse>> lowStock(
            @RequestParam(defaultValue = "5") Integer threshold, Pageable pageable) {
        return ResponseEntity.ok(productService.getLowStockProducts(threshold, pageable));
    }
}
