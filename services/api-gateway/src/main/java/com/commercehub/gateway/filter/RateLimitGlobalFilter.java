package com.commercehub.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "gateway.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitGlobalFilter implements GlobalFilter, Ordered {

    private static final String ROUTE_ID = "global";

    private final RedisRateLimiter rateLimiter;
    private final KeyResolver keyResolver;
    private final ObjectMapper objectMapper;

    public RateLimitGlobalFilter(
            RedisRateLimiter rateLimiter,
            KeyResolver keyResolver,
            ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.keyResolver = keyResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }
        return keyResolver.resolve(exchange)
                .flatMap(key -> rateLimiter.isAllowed(ROUTE_ID, key))
                .flatMap(response -> {
                    if (response.isAllowed()) {
                        return chain.filter(exchange);
                    }
                    return tooManyRequests(exchange);
                });
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 429,
                "error", "TOO_MANY_REQUESTS",
                "message", "Rate limit exceeded",
                "path", exchange.getRequest().getURI().getPath()
        );
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            bytes = "{\"error\":\"TOO_MANY_REQUESTS\",\"message\":\"Rate limit exceeded\"}"
                    .getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
