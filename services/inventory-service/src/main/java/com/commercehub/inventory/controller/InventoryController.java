package com.commercehub.inventory.controller;

import com.commercehub.inventory.dto.InventoryPageResponse;
import com.commercehub.inventory.dto.InventoryRequest;
import com.commercehub.inventory.dto.InventoryResponse;
import com.commercehub.inventory.dto.InventoryUpdateRequest;
import com.commercehub.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventory", description = "Inventory management endpoints")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create inventory record", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<InventoryResponse> create(@Valid @RequestBody InventoryRequest request) {
        InventoryResponse created = inventoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get inventory by product ID")
    public ResponseEntity<InventoryResponse> getByProductId(@PathVariable UUID productId) {
        return ResponseEntity.ok(inventoryService.getByProductId(productId));
    }

    @GetMapping
    @Operation(summary = "List inventory records with pagination")
    public ResponseEntity<InventoryPageResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(inventoryService.list(page, size));
    }

    @PatchMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update inventory record", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<InventoryResponse> update(
            @PathVariable UUID productId,
            @Valid @RequestBody InventoryUpdateRequest request
    ) {
        return ResponseEntity.ok(inventoryService.update(productId, request));
    }
}
