package com.example.ecommerce.service;

import com.example.ecommerce.dto.ProductImageDTOs;
import com.example.ecommerce.exception.InvalidOperationException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.model.ProductImage;
import com.example.ecommerce.repository.ProductImageRepository;
import com.example.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductImageService {

    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;

    public ProductImageDTOs.ProductImageResponse addImage(Long productId, Long sellerId, ProductImageDTOs.ProductImageCreateRequest request) {
        Product product = requireOwnedProduct(productId, sellerId);

        ProductImage image = ProductImage.builder()
            .product(product)
            .imageUrl(request.imageUrl())
            .altText(request.altText())
            .displayOrder(request.displayOrder() != null ? request.displayOrder() : nextDisplayOrder(productId))
            .build();

        return mapToResponse(productImageRepository.save(image));
    }

    @Transactional(readOnly = true)
    public List<ProductImageDTOs.ProductImageResponse> getImages(Long productId) {
        return productImageRepository.findByProductIdOrderByDisplayOrder(productId).stream()
            .map(this::mapToResponse)
            .toList();
    }

    public ProductImageDTOs.ProductImageResponse reorderImage(Long productId, Long imageId, Long sellerId, ProductImageDTOs.ProductImageReorderRequest request) {
        requireOwnedProduct(productId, sellerId);
        ProductImage image = productImageRepository.findById(imageId)
            .orElseThrow(() -> new ResourceNotFoundException("ProductImage", imageId));

        if (!image.getProduct().getId().equals(productId)) {
            throw new InvalidOperationException("Image does not belong to this product");
        }

        image.setDisplayOrder(request.displayOrder());
        return mapToResponse(productImageRepository.save(image));
    }

    public void deleteImage(Long productId, Long imageId, Long sellerId) {
        requireOwnedProduct(productId, sellerId);
        ProductImage image = productImageRepository.findById(imageId)
            .orElseThrow(() -> new ResourceNotFoundException("ProductImage", imageId));

        if (!image.getProduct().getId().equals(productId)) {
            throw new InvalidOperationException("Image does not belong to this product");
        }
        productImageRepository.delete(image);
    }

    private Product requireOwnedProduct(Long productId, Long sellerId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        if (!product.getSeller().getId().equals(sellerId)) {
            throw new InvalidOperationException("Seller can only manage images for their own products");
        }
        return product;
    }

    private int nextDisplayOrder(Long productId) {
        return (int) productImageRepository.countByProductId(productId);
    }

    private ProductImageDTOs.ProductImageResponse mapToResponse(ProductImage image) {
        return new ProductImageDTOs.ProductImageResponse(
            image.getId(),
            image.getProduct().getId(),
            image.getImageUrl(),
            image.getAltText(),
            image.getDisplayOrder(),
            image.getUploadedAt().toString()
        );
    }
}
