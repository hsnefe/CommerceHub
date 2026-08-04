package com.commercehub.messaging.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCancelledEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        UUID userId,
        List<OrderItemPayload> items
) {
}
