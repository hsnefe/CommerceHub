package com.commercehub.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.email-verification")
public record EmailVerificationProperties(
        boolean required
) {
}
