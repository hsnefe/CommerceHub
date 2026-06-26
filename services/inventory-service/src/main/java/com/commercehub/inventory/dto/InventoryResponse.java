package com.commercehub.inventory.dto;

import java.time.Instant;
import java.util.UUID;

public record InventoryResponse(
        UUID productId,
        int availableQuantity,
        int lowStockThreshold,
        boolean lowStock,
        Instant createdAt,
        Instant updatedAt
) {
}
