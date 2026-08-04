package com.commercehub.messaging.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        UUID userId,
        BigDecimal totalPrice,
        List<OrderItemPayload> items
) {
}
