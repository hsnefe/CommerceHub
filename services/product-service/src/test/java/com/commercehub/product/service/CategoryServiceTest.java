package com.commercehub.product.service;

import com.commercehub.product.dto.CategoryRequest;
import com.commercehub.product.dto.CategoryResponse;
import com.commercehub.product.entity.Category;
import com.commercehub.product.exception.ConflictException;
import com.commercehub.product.exception.NotFoundException;
import com.commercehub.product.repository.CategoryRepository;
import com.commercehub.product.repository.ProductRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category sampleCategory() {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Accessories");
        return category;
    }

    @Nested
    class Create {

        @Test
        void create_success() {
            when(categoryRepository.existsByName("Accessories")).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
                Category category = invocation.getArgument(0);
                category.setId(UUID.randomUUID());
                return category;
            });

            CategoryResponse response = categoryService.create(new CategoryRequest("Accessories"));

            assertThat(response.name()).isEqualTo("Accessories");
            assertThat(response.id()).isNotNull();
        }

        @Test
        void create_duplicateName_throwsConflict() {
            when(categoryRepository.existsByName("Accessories")).thenReturn(true);

            assertThatThrownBy(() -> categoryService.create(new CategoryRequest("Accessories")))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Category name already exists");
        }
    }

    @Nested
    class Delete {

        @Test
        void delete_withActiveProducts_throwsConflict() {
            UUID id = UUID.randomUUID();
            when(categoryRepository.findById(id)).thenReturn(Optional.of(sampleCategory()));
            when(productRepository.countByCategoryIdAndActiveTrue(id)).thenReturn(2L);

            assertThatThrownBy(() -> categoryService.delete(id))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Category has active products");
        }

        @Test
        void delete_success() {
            UUID id = UUID.randomUUID();
            Category category = sampleCategory();
            when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
            when(productRepository.countByCategoryIdAndActiveTrue(id)).thenReturn(0L);

            categoryService.delete(id);

            verify(categoryRepository).delete(category);
        }
    }

    @Nested
    class ListAll {

        @Test
        void listAll_returnsCategories() {
            when(categoryRepository.findAll()).thenReturn(List.of(sampleCategory()));

            List<CategoryResponse> responses = categoryService.listAll();

            assertThat(responses).hasSize(1);
            assertThat(responses.getFirst().name()).isEqualTo("Accessories");
        }
    }

    @Nested
    class Update {

        @Test
        void update_notFound_throws() {
            UUID id = UUID.randomUUID();
            when(categoryRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.update(id, new CategoryRequest("New")))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}
