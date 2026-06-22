package com.commercehub.auth.dto;

import com.commercehub.auth.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @ValidPassword String password
) {
}
