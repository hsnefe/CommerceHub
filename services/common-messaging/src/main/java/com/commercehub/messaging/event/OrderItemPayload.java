package com.commercehub.messaging.event;

import java.util.UUID;

public record OrderItemPayload(
        UUID productId,
        int quantity
) {
}
