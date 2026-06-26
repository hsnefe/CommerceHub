package com.commercehub.inventory.dto;

import jakarta.validation.constraints.Min;

public record QuantityAdjustmentRequest(
        @Min(1) int amount
) {
}
