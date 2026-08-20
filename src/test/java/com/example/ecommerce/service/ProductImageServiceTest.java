package com.example.ecommerce.service;

import com.example.ecommerce.dto.ProductImageDTOs;
import com.example.ecommerce.exception.InvalidOperationException;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.model.ProductImage;
import com.example.ecommerce.model.SellerProfile;
import com.example.ecommerce.repository.ProductImageRepository;
import com.example.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductImageService")
class ProductImageServiceTest {

    @Mock private ProductImageRepository productImageRepository;
    @Mock private ProductRepository productRepository;

    private ProductImageService productImageService;
    private Path uploadDir;

    @BeforeEach
    void setUp() throws Exception {
        productImageService = new ProductImageService(productImageRepository, productRepository);
        uploadDir = Files.createTempDirectory("product-image-service-test");
        ReflectionTestUtils.setField(productImageService, "uploadDir", uploadDir.toString());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private Product ownedProduct(Long sellerId) {
        SellerProfile seller = SellerProfile.builder().id(sellerId).build();
        return Product.builder().id(1L).seller(seller).build();
    }

    @Test
    @DisplayName("stores the file and records an image row with a served URL")
    void uploadImageStoresFileAndSavesRecord() throws Exception {
        Product product = ownedProduct(10L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productImageRepository.countByProductId(1L)).thenReturn(0L);
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(inv -> {
            ProductImage image = inv.getArgument(0);
            image.setId(99L);
            return image;
        });

        MultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", "fake-bytes".getBytes());

        ProductImageDTOs.ProductImageResponse response = productImageService.uploadImage(1L, 10L, file, "front view");

        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.altText()).isEqualTo("front view");
        assertThat(response.imageUrl()).contains("/uploads/products/").endsWith(".png");
        assertThat(Files.list(uploadDir.resolve("products")).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("rejects an upload for a product the seller does not own")
    void uploadImageRejectsNonOwner() {
        Product product = ownedProduct(10L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        MultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", "fake-bytes".getBytes());

        assertThatThrownBy(() -> productImageService.uploadImage(1L, 999L, file, null))
            .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    @DisplayName("rejects an empty file")
    void uploadImageRejectsEmptyFile() {
        Product product = ownedProduct(10L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        MultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> productImageService.uploadImage(1L, 10L, file, null))
            .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    @DisplayName("rejects a disallowed content type")
    void uploadImageRejectsDisallowedContentType() {
        Product product = ownedProduct(10L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        MultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "fake-bytes".getBytes());

        assertThatThrownBy(() -> productImageService.uploadImage(1L, 10L, file, null))
            .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    @DisplayName("deleting an image also removes its file from the uploads dir")
    void deleteImageRemovesUploadedFile() throws Exception {
        Product product = ownedProduct(10L);
        Path productsDir = Files.createDirectories(uploadDir.resolve("products"));
        Path storedFile = Files.createFile(productsDir.resolve("abc123.png"));
        ProductImage image = ProductImage.builder().id(5L).product(product)
            .imageUrl("http://localhost:8082/uploads/products/abc123.png").build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productImageRepository.findById(5L)).thenReturn(Optional.of(image));

        productImageService.deleteImage(1L, 5L, 10L);

        assertThat(Files.exists(storedFile)).isFalse();
    }

    @Test
    @DisplayName("deleting a manually-added image (not one of our uploads) does not touch the filesystem")
    void deleteImageIgnoresNonUploadedUrl() {
        Product product = ownedProduct(10L);
        ProductImage image = ProductImage.builder().id(5L).product(product)
            .imageUrl("https://cdn.example.com/photo.png").build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productImageRepository.findById(5L)).thenReturn(Optional.of(image));

        productImageService.deleteImage(1L, 5L, 10L);
        // No exception - the URL doesn't point at our uploads dir, so it's a no-op.
    }

    @Test
    @DisplayName("refuses to delete a file outside the uploads dir via a crafted imageUrl")
    void deleteImageRefusesPathTraversal() throws Exception {
        Product product = ownedProduct(10L);
        Path outsideFile = Files.createTempFile("outside-uploads-", ".txt");
        ProductImage image = ProductImage.builder().id(5L).product(product)
            .imageUrl("http://localhost:8082/uploads/products/../../../../" + outsideFile.getFileName()).build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productImageRepository.findById(5L)).thenReturn(Optional.of(image));

        productImageService.deleteImage(1L, 5L, 10L);

        assertThat(Files.exists(outsideFile)).isTrue();
        Files.deleteIfExists(outsideFile);
    }
}
