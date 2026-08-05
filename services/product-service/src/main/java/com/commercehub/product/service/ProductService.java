package com.commercehub.product.service;

import com.commercehub.product.config.ProductCacheProperties;
import com.commercehub.product.dto.ProductPageResponse;
import com.commercehub.product.dto.ProductRequest;
import com.commercehub.product.dto.ProductResponse;
import com.commercehub.product.dto.ProductSnapshotResponse;
import com.commercehub.product.dto.ProductSummaryResponse;
import com.commercehub.product.entity.Product;
import com.commercehub.product.exception.NotFoundException;
import com.commercehub.product.repository.ProductRepository;
import com.commercehub.product.repository.ProductSpecifications;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
public class ProductService {

    private static final String KEY_PREFIX = "product:";

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ProductCacheProperties cacheProperties;

    public ProductService(
            ProductRepository productRepository,
            CategoryService categoryService,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            ProductCacheProperties cacheProperties) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.cacheProperties = cacheProperties;
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        categoryService.getCategory(request.categoryId());

        Product product = new Product();
        applyRequest(product, request);
        Product saved = productRepository.save(product);
        return toProductResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(UUID id) {
        ProductResponse cached = getCached(id);
        if (cached != null) {
            return cached;
        }

        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        ProductResponse response = toProductResponse(product);
        putCache(id, response);
        return response;
    }

    @Transactional(readOnly = true)
    public ProductPageResponse list(UUID categoryId, String name, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Specification<Product> spec = ProductSpecifications.isActive();
        if (categoryId != null) {
            spec = spec.and(ProductSpecifications.hasCategoryId(categoryId));
        }
        if (name != null && !name.isBlank()) {
            spec = spec.and(ProductSpecifications.nameContains(name.trim()));
        }
        Page<Product> result = productRepository.findAll(spec, pageable);

        return new ProductPageResponse(
                result.getContent().stream().map(this::toProductSummary).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements()
        );
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        categoryService.getCategory(request.categoryId());
        applyRequest(product, request);
        ProductResponse response = toProductResponse(productRepository.save(product));
        evictCache(id);
        return response;
    }

    @Transactional
    public void delete(UUID id) {
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        product.setActive(false);
        productRepository.save(product);
        evictCache(id);
    }

    @Transactional(readOnly = true)
    public ProductSnapshotResponse getSnapshot(UUID id) {
        ProductResponse product = getById(id);
        return new ProductSnapshotResponse(product.id(), product.name(), product.price());
    }

    private ProductResponse getCached(UUID id) {
        String json = redisTemplate.opsForValue().get(cacheKey(id));
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ProductResponse.class);
        } catch (JsonProcessingException e) {
            evictCache(id);
            return null;
        }
    }

    private void putCache(UUID id, ProductResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(
                    cacheKey(id),
                    json,
                    Duration.ofSeconds(cacheProperties.ttlSeconds())
            );
        } catch (JsonProcessingException e) {
            // Skip caching on serialization failure
        }
    }

    private void evictCache(UUID id) {
        redisTemplate.delete(cacheKey(id));
    }

    private String cacheKey(UUID id) {
        return KEY_PREFIX + id;
    }

    private void applyRequest(Product product, ProductRequest request) {
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setCategoryId(request.categoryId());
        product.setCurrency("TRY");
    }

    private ProductResponse toProductResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCurrency(),
                product.getCategoryId()
        );
    }

    private ProductSummaryResponse toProductSummary(Product product) {
        return new ProductSummaryResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getCurrency()
        );
    }
}
