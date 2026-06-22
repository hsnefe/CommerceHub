package com.commercehub.auth.dto;

import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        List<String> roles
) {
}
