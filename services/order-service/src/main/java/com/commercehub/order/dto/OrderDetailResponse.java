package com.commercehub.order.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderDetailResponse(
        UUID orderId,
        UUID userId,
        String status,
        BigDecimal totalPrice,
        List<OrderItemResponse> items
) {
    public record OrderItemResponse(
            UUID productId,
            String productName,
            BigDecimal unitPrice,
            int quantity
    ) {
    }
}
