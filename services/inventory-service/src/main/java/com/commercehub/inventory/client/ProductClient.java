package com.commercehub.inventory.client;

import com.commercehub.inventory.config.ProductServiceProperties;
import com.commercehub.inventory.dto.ProductSnapshotResponse;
import com.commercehub.inventory.exception.NotFoundException;
import com.commercehub.inventory.exception.ServiceUnavailableException;
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

    public void validateProductExists(UUID productId) {
        try {
            restClient.get()
                    .uri(properties.getBaseUrl() + "/internal/products/{productId}", productId)
                    .retrieve()
                    .body(ProductSnapshotResponse.class);
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
