package com.commercehub.gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

class IpKeyResolverTest {

    private KeyResolver ipKeyResolver;

    @BeforeEach
    void setUp() {
        ipKeyResolver = new RateLimitConfig().ipKeyResolver();
    }

    @Test
    void resolvesFirstXForwardedForAddress() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/products")
                .header("X-Forwarded-For", "203.0.113.1, 70.41.3.18")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        String key = ipKeyResolver.resolve(exchange).block();

        assertThat(key).isEqualTo("203.0.113.1");
    }

    @Test
    void resolvesRemoteAddressWhenNoForwardedHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/products")
                .remoteAddress(new InetSocketAddress("192.168.1.10", 54321))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        String key = ipKeyResolver.resolve(exchange).block();

        assertThat(key).isEqualTo("192.168.1.10");
    }

    @Test
    void fallsBackToUnknownWhenNoAddressAvailable() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/products").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        String key = ipKeyResolver.resolve(exchange).block();

        assertThat(key).isEqualTo("unknown");
    }
}
