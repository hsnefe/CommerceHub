package com.commercehub.product.service;

import com.commercehub.product.dto.CategoryRequest;
import com.commercehub.product.dto.CategoryResponse;
import com.commercehub.product.entity.Category;
import com.commercehub.product.exception.ConflictException;
import com.commercehub.product.exception.NotFoundException;
import com.commercehub.product.repository.CategoryRepository;
import com.commercehub.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new ConflictException("Category name already exists");
        }

        Category category = new Category();
        category.setName(request.name());
        Category saved = categoryRepository.save(category);
        return toCategoryResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listAll() {
        return categoryRepository.findAll().stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));

        if (categoryRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new ConflictException("Category name already exists");
        }

        category.setName(request.name());
        return toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));

        if (productRepository.countByCategoryIdAndActiveTrue(id) > 0) {
            throw new ConflictException("Category has active products");
        }

        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public Category getCategory(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }

    private CategoryResponse toCategoryResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}
