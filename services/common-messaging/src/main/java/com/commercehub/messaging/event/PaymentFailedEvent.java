package com.commercehub.messaging.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        String reason
) {
}
