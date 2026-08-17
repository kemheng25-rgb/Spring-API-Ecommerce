package com.example.ecommerce.service;

import com.example.ecommerce.dto.CategoryDTOs;
import com.example.ecommerce.exception.DuplicateResourceException;
import com.example.ecommerce.exception.InvalidOperationException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.model.Category;
import com.example.ecommerce.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryDTOs.CategoryResponse createCategory(CategoryDTOs.CategoryCreateRequest request) {
        Category parent = null;
        if (request.parentId() != null) {
            parent = categoryRepository.findById(request.parentId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.parentId()));
        }

        categoryRepository.findByCategoryNameAndParentCategoryId(request.categoryName(), request.parentId())
            .ifPresent(existing -> {
                throw new DuplicateResourceException("Category", "categoryName", request.categoryName());
            });

        Category category = Category.builder()
            .parentCategory(parent)
            .categoryName(request.categoryName())
            .categoryDescription(request.categoryDescription())
            .isActive(true)
            .displayOrder(request.displayOrder() != null ? request.displayOrder() : 0)
            .build();

        return mapToResponse(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public CategoryDTOs.CategoryResponse getCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
        return mapToResponse(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryDTOs.CategoryResponse> getRootCategories() {
        return categoryRepository.findByParentCategoryIsNullAndIsActive(true).stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<CategoryDTOs.CategoryResponse> getSubcategories(Long parentId, Pageable pageable) {
        return categoryRepository.findByParentCategoryIdAndIsActive(parentId, true, pageable)
            .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<CategoryDTOs.CategoryResponse> getAllActive(Pageable pageable) {
        return categoryRepository.findByIsActive(true, pageable)
            .map(this::mapToResponse);
    }

    public CategoryDTOs.CategoryResponse updateCategory(Long categoryId, CategoryDTOs.CategoryUpdateRequest request) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));

        category.setCategoryName(request.categoryName());
        category.setCategoryDescription(request.categoryDescription());
        if (request.displayOrder() != null) {
            category.setDisplayOrder(request.displayOrder());
        }
        if (request.isActive() != null) {
            category.setIsActive(request.isActive());
        }

        return mapToResponse(categoryRepository.save(category));
    }

    public void deactivateCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));

        // Categories are never hard-deleted (products and child categories reference them,
        // mirroring the RESTRICT delete rule in the schema) - deactivate instead.
        if (!category.getProducts().isEmpty()) {
            throw new InvalidOperationException("Cannot deactivate a category with active products; move or delist them first");
        }
        category.setIsActive(false);
        categoryRepository.save(category);
    }

    private CategoryDTOs.CategoryResponse mapToResponse(Category category) {
        return new CategoryDTOs.CategoryResponse(
            category.getId(),
            category.getParentCategory() != null ? category.getParentCategory().getId() : null,
            category.getCategoryName(),
            category.getCategoryDescription(),
            category.getIsActive(),
            category.getDisplayOrder(),
            category.getProducts().size(),
            category.getCreatedAt().toString()
        );
    }
}
