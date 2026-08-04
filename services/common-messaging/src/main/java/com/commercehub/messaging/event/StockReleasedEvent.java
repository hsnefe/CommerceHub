package com.commercehub.messaging.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StockReleasedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        List<OrderItemPayload> items
) {
}
