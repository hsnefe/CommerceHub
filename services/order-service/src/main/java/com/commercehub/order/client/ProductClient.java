package com.commercehub.order.client;

import com.commercehub.order.config.ProductServiceProperties;
import com.commercehub.order.dto.ProductSnapshotResponse;
import com.commercehub.order.exception.NotFoundException;
import com.commercehub.order.exception.ServiceUnavailableException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class ProductClient {

    private final RestClient restClient;
    private final ProductServiceProperties properties;

    public ProductClient(RestClient restClient, ProductServiceProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

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
        } catch (RestClientException ex) {
            throw new ServiceUnavailableException("Product service is unavailable");
        }
    }
}
