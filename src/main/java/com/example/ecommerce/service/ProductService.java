package com.example.ecommerce.service;

import com.example.ecommerce.dto.ProductDTOs;
import com.example.ecommerce.exception.DuplicateResourceException;
import com.example.ecommerce.exception.InvalidOperationException;
import com.example.ecommerce.exception.OutOfStockException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.model.Category;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.model.SellerProfile;
import com.example.ecommerce.repository.CategoryRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.SellerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    
    private final ProductRepository productRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final CategoryRepository categoryRepository;
    
    public ProductDTOs.ProductResponse createProduct(Long sellerId, ProductDTOs.ProductCreateRequest request) {
        // Verify seller exists
        SellerProfile seller = sellerProfileRepository.findById(sellerId)
            .orElseThrow(() -> new ResourceNotFoundException("SellerProfile", sellerId));
        
        // Verify seller is verified
        if (seller.getVerificationStatus() != SellerProfile.VerificationStatus.VERIFIED) {
            throw new InvalidOperationException("Seller must be verified before listing products");
        }
        
        // Check for duplicate SKU
        if (productRepository.findBySku(request.sku()).isPresent()) {
            throw new DuplicateResourceException("Product", "sku", request.sku());
        }
        
        // Verify category exists
        Category category = categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));
        
        Product product = Product.builder()
            .seller(seller)
            .category(category)
            .productName(request.productName())
            .productDescription(request.productDescription())
            .sku(request.sku())
            .price(request.price())
            .currency("USD")
            .stockQuantity(request.stockQuantity())
            .productStatus(Product.ProductStatus.ACTIVE)
            .discountPercentage(request.discountPercentage() != null ? request.discountPercentage() : BigDecimal.ZERO)
            .build();
        
        Product savedProduct = productRepository.save(product);
        return mapToProductResponse(savedProduct);
    }
    
    @Transactional(readOnly = true)
    public ProductDTOs.ProductResponse getProduct(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        
        // Increment views count
        product.setViewsCount(product.getViewsCount() + 1);
        productRepository.save(product);
        
        return mapToProductResponse(product);
    }
    
    public ProductDTOs.ProductResponse updateProduct(Long productId, Long sellerId, 
                                                     ProductDTOs.ProductUpdateRequest request) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        
        // Verify seller owns this product
        if (!product.getSeller().getId().equals(sellerId)) {
            throw new InvalidOperationException("Seller can only update their own products");
        }
        
        product.setProductName(request.productName());
        product.setProductDescription(request.productDescription());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        if (request.discountPercentage() != null) {
            product.setDiscountPercentage(request.discountPercentage());
        }

        if (request.productStatus() != null) {
            product.setProductStatus(Product.ProductStatus.valueOf(request.productStatus()));
        }
        
        Product updatedProduct = productRepository.save(product);
        return mapToProductResponse(updatedProduct);
    }
    
    @Transactional(readOnly = true)
    public Page<ProductDTOs.ProductListResponse> getProductsByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findByProductStatusAndCategoryId(Product.ProductStatus.ACTIVE, categoryId, pageable)
            .map(this::mapToProductListResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<ProductDTOs.ProductListResponse> getProductsBySeller(Long sellerId, Pageable pageable) {
        return productRepository.findBySellerIdAndProductStatus(sellerId, Product.ProductStatus.ACTIVE, pageable)
            .map(this::mapToProductListResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<ProductDTOs.ProductListResponse> getFeaturedProducts(Pageable pageable) {
        return productRepository.findByProductStatusOrderByAverageRatingDesc(Product.ProductStatus.ACTIVE, pageable)
            .map(this::mapToProductListResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<ProductDTOs.ProductListResponse> getDiscountedProducts(Pageable pageable) {
        return productRepository.findActiveDiscountedProducts(Product.ProductStatus.ACTIVE, LocalDateTime.now(), pageable)
            .map(this::mapToProductListResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<ProductDTOs.ProductListResponse> getLowStockProducts(Integer threshold, Pageable pageable) {
        return productRepository.findLowStockProducts(Product.ProductStatus.ACTIVE, threshold, pageable)
            .map(this::mapToProductListResponse);
    }
    
    public void reduceStock(Long productId, Integer quantity) {
        // Pessimistic lock: two concurrent checkouts for the last unit must not both succeed.
        Product product = productRepository.findByIdForUpdate(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        if (product.getStockQuantity() < quantity) {
            throw new OutOfStockException(productId, quantity, product.getStockQuantity());
        }

        product.setStockQuantity(product.getStockQuantity() - quantity);
        productRepository.save(product);
    }
    
    public void increaseStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        
        product.setStockQuantity(product.getStockQuantity() + quantity);
        productRepository.save(product);
    }
    
    private ProductDTOs.ProductResponse mapToProductResponse(Product product) {
        return new ProductDTOs.ProductResponse(
            product.getId(),
            product.getProductName(),
            product.getProductDescription(),
            product.getSku(),
            product.getPrice(),
            product.getStockQuantity(),
            product.getProductStatus().toString(),
            product.getAverageRating(),
            product.getTotalReviews(),
            product.getDiscountPercentage(),
            product.getCategory().getId(),
            product.getSeller().getId(),
            product.getSeller().getShopName(),
            product.getViewsCount(),
            product.getCreatedAt().toString()
        );
    }
    
    private ProductDTOs.ProductListResponse mapToProductListResponse(Product product) {
        return new ProductDTOs.ProductListResponse(
            product.getId(),
            product.getProductName(),
            product.getPrice(),
            product.getProductStatus().toString(),
            product.getAverageRating(),
            product.getTotalReviews(),
            product.getDiscountPercentage(),
            product.getCategory().getCategoryName(),
            product.getSeller().getShopName()
        );
    }
}
