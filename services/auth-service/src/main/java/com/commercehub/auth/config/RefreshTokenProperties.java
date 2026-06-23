package com.commercehub.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.refresh-token")
public record RefreshTokenProperties(
        int expirationDays
) {
}
