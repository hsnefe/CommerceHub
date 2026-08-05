package com.commercehub.analytics.dto;

import java.time.Instant;
import java.util.UUID;

public record AnalyticsEventResponse(
        UUID id,
        UUID eventId,
        String eventType,
        UUID orderId,
        String payload,
        Instant receivedAt
) {
}
