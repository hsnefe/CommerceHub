package com.commercehub.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InventoryRequest(
        @NotNull UUID productId,
        @Min(0) int availableQuantity,
        @Min(0) int lowStockThreshold
) {
}
