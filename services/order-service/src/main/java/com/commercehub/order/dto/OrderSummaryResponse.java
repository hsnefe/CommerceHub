package com.commercehub.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID orderId,
        String status,
        BigDecimal totalPrice
) {
}
