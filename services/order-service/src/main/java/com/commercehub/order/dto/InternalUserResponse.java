package com.commercehub.order.dto;

import java.util.UUID;

public record InternalUserResponse(
        UUID id,
        String email
) {
}
