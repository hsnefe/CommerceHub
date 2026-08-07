package com.commercehub.order.client;

import com.commercehub.order.OrderServiceApplication;
import com.commercehub.order.config.RestClientConfig;
import com.commercehub.order.exception.NotFoundException;
import com.commercehub.order.exception.TransientServiceUnavailableException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        classes = ServiceClientResilienceIntegrationTest.TestApplication.class,
        properties = "eureka.client.enabled=false"
)
@ActiveProfiles("test")
class ServiceClientResilienceIntegrationTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final StubServer STUB_SERVER = new StubServer();

    @Autowired
    private ProductClient productClient;

    @Autowired
    private AuthClient authClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private MeterRegistry meterRegistry;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("commercehub.product-service.base-url", () -> STUB_SERVER.baseUrl());
        registry.add("commercehub.auth-service.base-url", () -> STUB_SERVER.baseUrl());
        registry.add("resilience4j.retry.configs.serviceClient.wait-duration", () -> "1ms");
        registry.add("resilience4j.circuitbreaker.configs.serviceClient.sliding-window-size", () -> "2");
        registry.add("resilience4j.circuitbreaker.configs.serviceClient.minimum-number-of-calls", () -> "2");
    }

    @BeforeEach
    void reset() {
        STUB_SERVER.reset();
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
    }

    @AfterAll
    static void stopStubServer() {
        STUB_SERVER.stop();
    }

    @Test
    void retriesTransient503AndReturnsSuccessfulResponse() {
        STUB_SERVER.enqueue("product", 503, 503, 200);

        assertThat(productClient.getProductSnapshot(ID).id()).isEqualTo(ID);
        assertThat(STUB_SERVER.count("product")).isEqualTo(3);
    }

    @Test
    void doesNotRetry404() {
        STUB_SERVER.enqueue("product", 404);

        assertThatThrownBy(() -> productClient.getProductSnapshot(ID))
                .isInstanceOf(NotFoundException.class);
        assertThat(STUB_SERVER.count("product")).isEqualTo(1);
    }

    @Test
    void exhaustsThreeAttemptsForPersistent503() {
        STUB_SERVER.enqueue("product", 503, 503, 503);

        assertThatThrownBy(() -> productClient.getProductSnapshot(ID))
                .isInstanceOf(TransientServiceUnavailableException.class);
        assertThat(STUB_SERVER.count("product")).isEqualTo(3);
    }

    @Test
    void openProductCircuitBlocksNetworkAndDoesNotAffectAuthCircuit() {
        STUB_SERVER.enqueue("product", 503, 503, 503, 503, 503, 503);
        assertThatThrownBy(() -> productClient.getProductSnapshot(ID))
                .isInstanceOf(TransientServiceUnavailableException.class);
        assertThatThrownBy(() -> productClient.getProductSnapshot(ID))
                .isInstanceOf(TransientServiceUnavailableException.class);
        assertThat(circuitBreakerRegistry.circuitBreaker("orderProduct").getState())
                .isEqualTo(CircuitBreaker.State.OPEN);

        int requestsBeforeOpenCall = STUB_SERVER.count("product");
        assertThatThrownBy(() -> productClient.getProductSnapshot(ID))
                .isInstanceOf(CallNotPermittedException.class);
        assertThat(STUB_SERVER.count("product")).isEqualTo(requestsBeforeOpenCall);

        STUB_SERVER.enqueue("auth", 200);
        assertThat(authClient.getUserEmail(ID)).isEqualTo("user@example.com");
        assertThat(circuitBreakerRegistry.circuitBreaker("orderAuth").getState())
                .isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void registersCircuitBreakerMetrics() {
        assertThat(meterRegistry.find("resilience4j.circuitbreaker.state")
                .tag("name", "orderProduct")
                .meters())
                .isNotEmpty();
    }

    private static final class StubServer {

        private final HttpServer server;
        private final Map<String, Queue<Integer>> statuses = new ConcurrentHashMap<>();
        private final Map<String, AtomicInteger> counts = new ConcurrentHashMap<>();

        private StubServer() {
            try {
                server = HttpServer.create(new InetSocketAddress(0), 0);
            } catch (IOException ex) {
                throw new IllegalStateException("Could not start HTTP stub server", ex);
            }
            server.createContext("/", this::handle);
            server.setExecutor(Executors.newCachedThreadPool(runnable -> {
                Thread thread = new Thread(runnable);
                thread.setDaemon(true);
                return thread;
            }));
            server.start();
        }

        private String baseUrl() {
            return "http://localhost:" + server.getAddress().getPort();
        }

        private void enqueue(String service, int... responseStatuses) {
            Queue<Integer> queue = statuses.computeIfAbsent(service, ignored -> new ConcurrentLinkedQueue<>());
            for (int status : responseStatuses) {
                queue.add(status);
            }
        }

        private int count(String service) {
            return counts.getOrDefault(service, new AtomicInteger()).get();
        }

        private void reset() {
            statuses.clear();
            counts.clear();
        }

        private void stop() {
            server.stop(0);
        }

        private void handle(HttpExchange exchange) throws IOException {
            String service = exchange.getRequestURI().getPath().contains("/internal/users/")
                    ? "auth"
                    : "product";
            counts.computeIfAbsent(service, ignored -> new AtomicInteger()).incrementAndGet();
            int status = statuses.getOrDefault(service, new ConcurrentLinkedQueue<>())
                    .poll();
            byte[] body = responseBody(service, status).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        }

        private String responseBody(String service, int status) {
            if (status != 200) {
                return "{\"error\":\"stub error\"}";
            }
            if ("auth".equals(service)) {
                return "{\"id\":\"" + ID + "\",\"email\":\"user@example.com\"}";
            }
            return "{\"id\":\"" + ID + "\",\"name\":\"Test Product\",\"price\":10.00}";
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            RabbitAutoConfiguration.class
    })
    @ConfigurationPropertiesScan(basePackageClasses = OrderServiceApplication.class)
    @Import({RestClientConfig.class, ProductClient.class, AuthClient.class})
    static class TestApplication {
    }
}
