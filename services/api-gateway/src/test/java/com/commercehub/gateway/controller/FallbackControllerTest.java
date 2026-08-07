package com.commercehub.gateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

class FallbackControllerTest {

    private final WebTestClient webTestClient = WebTestClient
            .bindToController(new FallbackController())
            .build();

    @Test
    void returnsStandardServiceUnavailableResponse() {
        webTestClient.get()
                .uri("/fallback/product-service")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.error").isEqualTo("SERVICE_UNAVAILABLE")
                .jsonPath("$.service").isEqualTo("product-service")
                .jsonPath("$.path").isEqualTo("/fallback/product-service");
    }

    @Test
    void handlesPostFallbackWithoutRetryingRequest() {
        webTestClient.post()
                .uri("/fallback/order-service")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.service").isEqualTo("order-service");
    }
}
