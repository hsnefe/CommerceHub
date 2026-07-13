package com.commercehub.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateOrderResponse(
        UUID orderId,
        String status,
        BigDecimal totalPrice,
        Instant createdAt
) {
}
