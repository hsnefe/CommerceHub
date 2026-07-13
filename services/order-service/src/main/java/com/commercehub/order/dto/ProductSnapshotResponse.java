package com.commercehub.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSnapshotResponse(
        UUID id,
        String name,
        BigDecimal price
) {
}
