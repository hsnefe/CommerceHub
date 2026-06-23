package com.commercehub.product.service;

import com.commercehub.product.dto.ProductPageResponse;
import com.commercehub.product.dto.ProductRequest;
import com.commercehub.product.dto.ProductResponse;
import com.commercehub.product.dto.ProductSnapshotResponse;
import com.commercehub.product.entity.Category;
import com.commercehub.product.entity.Product;
import com.commercehub.product.exception.NotFoundException;
import com.commercehub.product.repository.ProductRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.commercehub.product.repository.ProductSpecifications;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private ProductService productService;

    private final UUID categoryId = UUID.randomUUID();

    private Category sampleCategory() {
        Category category = new Category();
        category.setId(categoryId);
        category.setName("Accessories");
        return category;
    }

    private Product sampleProduct() {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setCategoryId(categoryId);
        product.setName("Gaming Mouse");
        product.setDescription("Wireless");
        product.setPrice(new BigDecimal("799.99"));
        product.setCurrency("TRY");
        product.setActive(true);
        return product;
    }

    private ProductRequest sampleRequest() {
        return new ProductRequest("Gaming Mouse", "Wireless", new BigDecimal("799.99"), categoryId);
    }

    @Nested
    class Create {

        @Test
        void create_success() {
            when(categoryService.getCategory(categoryId)).thenReturn(sampleCategory());
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
                Product product = invocation.getArgument(0);
                product.setId(UUID.randomUUID());
                return product;
            });

            ProductResponse response = productService.create(sampleRequest());

            assertThat(response.name()).isEqualTo("Gaming Mouse");
            assertThat(response.price()).isEqualByComparingTo("799.99");
            assertThat(response.currency()).isEqualTo("TRY");
        }
    }

    @Nested
    class GetById {

        @Test
        void getById_notFound() {
            UUID id = UUID.randomUUID();
            when(productRepository.findByIdAndActiveTrue(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getById(id))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void getById_success() {
            Product product = sampleProduct();
            when(productRepository.findByIdAndActiveTrue(product.getId())).thenReturn(Optional.of(product));

            ProductResponse response = productService.getById(product.getId());

            assertThat(response.id()).isEqualTo(product.getId());
            assertThat(response.categoryId()).isEqualTo(categoryId);
        }
    }

    @Nested
    class ListProducts {

        @Test
        void list_returnsPage() {
            Product product = sampleProduct();
            when(productRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 20))))
                    .thenReturn(new PageImpl<>(java.util.List.of(product), PageRequest.of(0, 20), 1));

            ProductPageResponse page = productService.list(null, null, 0, 20);

            assertThat(page.content()).hasSize(1);
            assertThat(page.totalElements()).isEqualTo(1);
            assertThat(page.page()).isZero();
            assertThat(page.size()).isEqualTo(20);
        }
    }

    @Nested
    class Delete {

        @Test
        void delete_softDeletesProduct() {
            Product product = sampleProduct();
            when(productRepository.findByIdAndActiveTrue(product.getId())).thenReturn(Optional.of(product));
            when(productRepository.save(product)).thenReturn(product);

            productService.delete(product.getId());

            assertThat(product.isActive()).isFalse();
            verify(productRepository).save(product);
        }
    }

    @Nested
    class Snapshot {

        @Test
        void getSnapshot_returnsPriceAndName() {
            Product product = sampleProduct();
            when(productRepository.findByIdAndActiveTrue(product.getId())).thenReturn(Optional.of(product));

            ProductSnapshotResponse snapshot = productService.getSnapshot(product.getId());

            assertThat(snapshot.id()).isEqualTo(product.getId());
            assertThat(snapshot.name()).isEqualTo("Gaming Mouse");
            assertThat(snapshot.price()).isEqualByComparingTo("799.99");
        }
    }
}
