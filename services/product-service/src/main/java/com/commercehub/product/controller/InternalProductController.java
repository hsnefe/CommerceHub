package com.commercehub.product.controller;

import com.commercehub.product.dto.ProductSnapshotResponse;
import com.commercehub.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/products")
@Tag(name = "Internal Products", description = "Service-to-service product endpoints")
public class InternalProductController {

    private final ProductService productService;

    public InternalProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get product snapshot for order processing")
    public ResponseEntity<ProductSnapshotResponse> getSnapshot(@PathVariable UUID productId) {
        return ResponseEntity.ok(productService.getSnapshot(productId));
    }
}
