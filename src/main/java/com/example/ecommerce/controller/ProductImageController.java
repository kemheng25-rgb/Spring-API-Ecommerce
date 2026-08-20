package com.example.ecommerce.controller;

import com.example.ecommerce.dto.ProductImageDTOs;
import com.example.ecommerce.service.ProductImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products/{productId}/images")
@RequiredArgsConstructor
@Tag(name = "Product Images", description = "Seller-managed product image gallery")
public class ProductImageController {

    private final ProductImageService productImageService;

    @PostMapping
    @Operation(summary = "Add an image to a product this seller owns")
    public ResponseEntity<ProductImageDTOs.ProductImageResponse> add(
            @PathVariable Long productId, @RequestParam Long sellerId,
            @Valid @RequestBody ProductImageDTOs.ProductImageCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productImageService.addImage(productId, sellerId, request));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload an image file for a product this seller owns")
    public ResponseEntity<ProductImageDTOs.ProductImageResponse> upload(
            @PathVariable Long productId, @RequestParam Long sellerId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String altText) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productImageService.uploadImage(productId, sellerId, file, altText));
    }

    @GetMapping
    @Operation(summary = "List a product's images, in display order")
    public ResponseEntity<List<ProductImageDTOs.ProductImageResponse>> list(@PathVariable Long productId) {
        return ResponseEntity.ok(productImageService.getImages(productId));
    }

    @PutMapping("/{imageId}")
    @Operation(summary = "Reorder an image")
    public ResponseEntity<ProductImageDTOs.ProductImageResponse> reorder(
            @PathVariable Long productId, @PathVariable Long imageId, @RequestParam Long sellerId,
            @Valid @RequestBody ProductImageDTOs.ProductImageReorderRequest request) {
        return ResponseEntity.ok(productImageService.reorderImage(productId, imageId, sellerId, request));
    }

    @DeleteMapping("/{imageId}")
    @Operation(summary = "Remove an image")
    public ResponseEntity<Void> delete(@PathVariable Long productId, @PathVariable Long imageId, @RequestParam Long sellerId) {
        productImageService.deleteImage(productId, imageId, sellerId);
        return ResponseEntity.noContent().build();
    }
}
