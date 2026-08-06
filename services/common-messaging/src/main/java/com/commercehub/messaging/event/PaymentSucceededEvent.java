package com.commercehub.messaging.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentSucceededEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        UUID userId,
        BigDecimal amount
) {
}
