package com.commercehub.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        int replenishRate,
        int burstCapacity,
        int requestedTokens
) {
    public RateLimitProperties {
        if (replenishRate <= 0) {
            replenishRate = 10;
        }
        if (burstCapacity <= 0) {
            burstCapacity = 20;
        }
        if (requestedTokens <= 0) {
            requestedTokens = 1;
        }
    }
}
