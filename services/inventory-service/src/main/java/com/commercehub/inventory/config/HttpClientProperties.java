package com.commercehub.inventory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "commercehub.http-client")
public record HttpClientProperties(
        int connectTimeoutMs,
        int readTimeoutMs
) {
    public HttpClientProperties {
        if (connectTimeoutMs <= 0) {
            connectTimeoutMs = 500;
        }
        if (readTimeoutMs <= 0) {
            readTimeoutMs = 2000;
        }
    }
}
