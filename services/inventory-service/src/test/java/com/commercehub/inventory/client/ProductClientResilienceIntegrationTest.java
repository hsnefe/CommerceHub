package com.commercehub.inventory.client;

import com.commercehub.inventory.InventoryServiceApplication;
import com.commercehub.inventory.config.RestClientConfig;
import com.commercehub.inventory.exception.NotFoundException;
import com.commercehub.messaging.CommonMessagingAutoConfiguration;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
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
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        classes = ProductClientResilienceIntegrationTest.TestApplication.class,
        properties = "eureka.client.enabled=false"
)
@ActiveProfiles("test")
class ProductClientResilienceIntegrationTest {

    private static final UUID ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final StubServer STUB_SERVER = new StubServer();

    @Autowired
    private ProductClient productClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("commercehub.product-service.base-url", STUB_SERVER::baseUrl);
        registry.add("resilience4j.retry.configs.serviceClient.wait-duration", () -> "1ms");
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
    void retriesTransientFailureBeforeValidatingProduct() {
        STUB_SERVER.enqueue(503, 200);

        assertThatCode(() -> productClient.validateProductExists(ID)).doesNotThrowAnyException();
        assertThat(STUB_SERVER.count()).isEqualTo(2);
    }

    @Test
    void treatsEmptySuccessfulResponseAsValidationFailureWithoutRetry() {
        STUB_SERVER.enqueue(204);

        assertThatThrownBy(() -> productClient.validateProductExists(ID))
                .isInstanceOf(NotFoundException.class);
        assertThat(STUB_SERVER.count()).isEqualTo(1);
    }

    private static final class StubServer {

        private final HttpServer server;
        private final Queue<Integer> statuses = new ConcurrentLinkedQueue<>();
        private final AtomicInteger count = new AtomicInteger();

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

        private void enqueue(int... responseStatuses) {
            for (int status : responseStatuses) {
                statuses.add(status);
            }
        }

        private int count() {
            return count.get();
        }

        private void reset() {
            statuses.clear();
            count.set(0);
        }

        private void stop() {
            server.stop(0);
        }

        private void handle(HttpExchange exchange) throws IOException {
            count.incrementAndGet();
            int status = statuses.remove();
            if (status == 204) {
                exchange.sendResponseHeaders(status, -1);
            } else {
                String response = status == 200
                        ? "{\"id\":\"" + ID + "\",\"name\":\"Test Product\",\"price\":10.00}"
                        : "{\"error\":\"stub error\"}";
                byte[] body = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, body.length);
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            RabbitAutoConfiguration.class,
            CommonMessagingAutoConfiguration.class
    })
    @ConfigurationPropertiesScan(basePackageClasses = InventoryServiceApplication.class)
    @Import({RestClientConfig.class, ProductClient.class})
    static class TestApplication {
    }
}
