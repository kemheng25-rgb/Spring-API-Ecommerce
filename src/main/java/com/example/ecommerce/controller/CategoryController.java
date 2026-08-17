package com.example.ecommerce.controller;

import com.example.ecommerce.dto.CategoryDTOs;
import com.example.ecommerce.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Hierarchical product catalog")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @Operation(summary = "Admin: create a category (or subcategory, via parentId)")
    public ResponseEntity<CategoryDTOs.CategoryResponse> create(@Valid @RequestBody CategoryDTOs.CategoryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(request));
    }

    @GetMapping
    @Operation(summary = "List root categories")
    public ResponseEntity<List<CategoryDTOs.CategoryResponse>> roots() {
        return ResponseEntity.ok(categoryService.getRootCategories());
    }

    @GetMapping("/active")
    @Operation(summary = "List all active categories, paginated")
    public ResponseEntity<Page<CategoryDTOs.CategoryResponse>> active(Pageable pageable) {
        return ResponseEntity.ok(categoryService.getAllActive(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a category")
    public ResponseEntity<CategoryDTOs.CategoryResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategory(id));
    }

    @GetMapping("/{id}/subcategories")
    @Operation(summary = "List a category's direct children")
    public ResponseEntity<Page<CategoryDTOs.CategoryResponse>> subcategories(@PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(categoryService.getSubcategories(id, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Admin: update a category")
    public ResponseEntity<CategoryDTOs.CategoryResponse> update(
            @PathVariable Long id, @Valid @RequestBody CategoryDTOs.CategoryUpdateRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Admin: deactivate a category (blocked while it still has products)")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        categoryService.deactivateCategory(id);
        return ResponseEntity.noContent().build();
    }
}
