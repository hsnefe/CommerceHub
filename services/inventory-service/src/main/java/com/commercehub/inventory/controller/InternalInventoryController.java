package com.commercehub.inventory.controller;

import com.commercehub.inventory.dto.InventoryResponse;
import com.commercehub.inventory.dto.QuantityAdjustmentRequest;
import com.commercehub.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/inventory")
@Tag(name = "Internal Inventory", description = "Service-to-service inventory endpoints")
public class InternalInventoryController {

    private final InventoryService inventoryService;

    public InternalInventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/{productId}/decrement")
    @Operation(summary = "Decrement stock for order processing")
    public ResponseEntity<InventoryResponse> decrement(
            @PathVariable UUID productId,
            @Valid @RequestBody QuantityAdjustmentRequest request
    ) {
        return ResponseEntity.ok(inventoryService.decrement(productId, request.amount()));
    }

    @PostMapping("/{productId}/increment")
    @Operation(summary = "Increment stock for order cancellation or returns")
    public ResponseEntity<InventoryResponse> increment(
            @PathVariable UUID productId,
            @Valid @RequestBody QuantityAdjustmentRequest request
    ) {
        return ResponseEntity.ok(inventoryService.increment(productId, request.amount()));
    }
}
