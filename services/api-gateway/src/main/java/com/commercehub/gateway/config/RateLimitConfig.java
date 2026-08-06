package com.commercehub.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.List;

@Configuration
public class RateLimitConfig {

    @Bean
    KeyResolver ipKeyResolver() {
        return exchange -> {
            String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return Mono.just(forwarded.split(",")[0].trim());
            }
            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
            if (remoteAddress != null && remoteAddress.getAddress() != null) {
                return Mono.just(remoteAddress.getAddress().getHostAddress());
            }
            return Mono.just("unknown");
        };
    }

    @Bean
    RedisRateLimiter redisRateLimiter(
            ReactiveStringRedisTemplate redisTemplate,
            RedisScript<List<Long>> redisRequestRateLimiterScript,
            ConfigurationService configurationService,
            RateLimitProperties properties) {
        RedisRateLimiter rateLimiter = new RedisRateLimiter(
                redisTemplate,
                redisRequestRateLimiterScript,
                configurationService
        );
        rateLimiter.getConfig().put(
                "global",
                new RedisRateLimiter.Config()
                        .setReplenishRate(properties.replenishRate())
                        .setBurstCapacity(properties.burstCapacity())
                        .setRequestedTokens(properties.requestedTokens())
        );
        return rateLimiter;
    }
}
