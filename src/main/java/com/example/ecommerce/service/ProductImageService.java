package com.example.ecommerce.service;

import com.example.ecommerce.dto.ProductImageDTOs;
import com.example.ecommerce.exception.InvalidOperationException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.model.ProductImage;
import com.example.ecommerce.repository.ProductImageRepository;
import com.example.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProductImageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final String UPLOAD_URL_MARKER = "/uploads/products/";

    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

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

    public ProductImageDTOs.ProductImageResponse uploadImage(Long productId, Long sellerId, MultipartFile file, String altText) {
        Product product = requireOwnedProduct(productId, sellerId);

        if (file == null || file.isEmpty()) {
            throw new InvalidOperationException("Image file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidOperationException("Image must be one of: " + ALLOWED_CONTENT_TYPES);
        }

        String extension = switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".gif";
        };
        String filename = UUID.randomUUID() + extension;

        try {
            Path targetDir = Path.of(uploadDir, "products").toAbsolutePath().normalize();
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store uploaded image", e);
        }

        String imageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/uploads/products/{filename}")
            .buildAndExpand(filename)
            .toUriString();

        ProductImage image = ProductImage.builder()
            .product(product)
            .imageUrl(imageUrl)
            .altText(altText)
            .displayOrder(nextDisplayOrder(productId))
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
        deleteUploadedFileIfPresent(image.getImageUrl());
    }

    /** No-op for a manually-added imageUrl (addImage) that doesn't point at our uploads dir. */
    private void deleteUploadedFileIfPresent(String imageUrl) {
        int idx = imageUrl.indexOf(UPLOAD_URL_MARKER);
        if (idx < 0) {
            return;
        }
        String filename = imageUrl.substring(idx + UPLOAD_URL_MARKER.length());
        if (filename.isBlank() || filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            log.warn("Refusing to delete uploaded file with suspicious name: {}", filename);
            return;
        }
        try {
            Path targetDir = Path.of(uploadDir, "products").toAbsolutePath().normalize();
            Path target = targetDir.resolve(filename).normalize();
            if (!target.startsWith(targetDir)) {
                log.warn("Refusing to delete file outside uploads dir: {}", target);
                return;
            }
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("Failed to delete uploaded image file {}: {}", filename, e.getMessage());
        }
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
