package com.commercehub.product.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "product.cache")
public record ProductCacheProperties(
        long ttlSeconds
) {
    public ProductCacheProperties {
        if (ttlSeconds <= 0) {
            ttlSeconds = 300;
        }
    }
}
