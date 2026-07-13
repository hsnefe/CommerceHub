package com.commercehub.order.dto;

import jakarta.validation.constraints.Min;

public record QuantityAdjustmentRequest(
        @Min(1) int amount
) {
}
