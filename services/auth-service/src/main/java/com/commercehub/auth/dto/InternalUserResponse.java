package com.commercehub.auth.dto;

import java.util.UUID;

public record InternalUserResponse(
        UUID id,
        String email
) {
}
