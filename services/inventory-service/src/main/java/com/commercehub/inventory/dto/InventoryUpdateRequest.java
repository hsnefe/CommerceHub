package com.commercehub.inventory.dto;

import jakarta.validation.constraints.Min;

public record InventoryUpdateRequest(
        @Min(0) Integer availableQuantity,
        @Min(0) Integer lowStockThreshold
) {
}
