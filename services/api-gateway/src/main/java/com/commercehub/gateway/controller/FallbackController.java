package com.commercehub.gateway.controller;

import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@RestController
public class FallbackController {

    @RequestMapping(path = "/fallback/{service}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> fallback(
            @PathVariable String service,
            ServerWebExchange exchange
    ) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "timestamp", LocalDateTime.now().toString(),
                        "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "error", "SERVICE_UNAVAILABLE",
                        "message", service + " is temporarily unavailable",
                        "service", service,
                        "path", originalPath(exchange)
                ));
    }

    @SuppressWarnings("unchecked")
    private String originalPath(ServerWebExchange exchange) {
        Set<URI> originalUris = exchange.getAttribute(
                ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR
        );
        if (originalUris == null || originalUris.isEmpty()) {
            return exchange.getRequest().getURI().getPath();
        }
        return originalUris.iterator().next().getPath();
    }
}
