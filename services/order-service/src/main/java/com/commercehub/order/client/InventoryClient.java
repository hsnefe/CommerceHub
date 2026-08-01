package com.commercehub.order.client;

import com.commercehub.order.config.InventoryServiceProperties;
import com.commercehub.order.dto.QuantityAdjustmentRequest;
import com.commercehub.order.exception.ConflictException;
import com.commercehub.order.exception.NotFoundException;
import com.commercehub.order.exception.ServiceUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class InventoryClient {

    private final RestClient restClient;
    private final InventoryServiceProperties properties;

    public InventoryClient(RestClient restClient, InventoryServiceProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public void decrement(UUID productId, int amount) {
        adjust(productId, amount, "decrement");
    }

    public void increment(UUID productId, int amount) {
        adjust(productId, amount, "increment");
    }

    private void adjust(UUID productId, int amount, String action) {
        try {
            restClient.post()
                    .uri(properties.getBaseUrl() + "/internal/inventory/{productId}/" + action, productId)
                    .body(new QuantityAdjustmentRequest(amount))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new NotFoundException("Inventory record not found");
            }
            if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                throw new ConflictException("Insufficient stock");
            }
            throw new ServiceUnavailableException("Inventory service is unavailable");
        } catch (RestClientException ex) {
            throw new ServiceUnavailableException("Inventory service is unavailable");
        }
    }
}
