package com.commercehub.order.client;

import com.commercehub.order.config.ProductServiceProperties;
import com.commercehub.order.dto.ProductSnapshotResponse;
import com.commercehub.order.exception.NotFoundException;
import com.commercehub.order.exception.ServiceUnavailableException;
import com.commercehub.order.exception.TransientServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.ResourceAccessException;

import java.util.UUID;

@Component
public class ProductClient {

    private final RestClient restClient;
    private final ProductServiceProperties properties;

    public ProductClient(RestClient restClient, ProductServiceProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @CircuitBreaker(name = "orderProduct")
    @Retry(name = "orderProduct")
    public ProductSnapshotResponse getProductSnapshot(UUID productId) {
        try {
            ProductSnapshotResponse snapshot = restClient.get()
                    .uri(properties.getBaseUrl() + "/internal/products/{productId}", productId)
                    .retrieve()
                    .body(ProductSnapshotResponse.class);
            if (snapshot == null) {
                throw new NotFoundException("Product not found");
            }
            return snapshot;
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new NotFoundException("Product not found");
            }
            throw new ServiceUnavailableException("Product service is unavailable");
        } catch (HttpServerErrorException ex) {
            if (isTransient(ex.getStatusCode())) {
                throw new TransientServiceUnavailableException("Product service is temporarily unavailable");
            }
            throw new ServiceUnavailableException("Product service is unavailable");
        } catch (ResourceAccessException ex) {
            throw new TransientServiceUnavailableException("Product service is temporarily unavailable");
        } catch (RestClientException ex) {
            throw new ServiceUnavailableException("Product service is unavailable");
        }
    }

    private boolean isTransient(HttpStatusCode statusCode) {
        int status = statusCode.value();
        return status == 502 || status == 503 || status == 504;
    }
}
