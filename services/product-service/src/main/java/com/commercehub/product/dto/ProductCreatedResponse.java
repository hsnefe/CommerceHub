package com.commercehub.product.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductCreatedResponse(
        UUID id,
        String name,
        BigDecimal price
) {
}
