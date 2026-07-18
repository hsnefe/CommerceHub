package com.commercehub.order.client;

import com.commercehub.order.config.NotificationServiceProperties;
import com.commercehub.order.dto.SendNotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final RestClient restClient;
    private final NotificationServiceProperties properties;

    public NotificationClient(RestClient restClient, NotificationServiceProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public void sendOrderCreated(String email, UUID orderId) {
        try {
            restClient.post()
                    .uri(properties.getBaseUrl() + "/api/v1/notifications")
                    .body(new SendNotificationRequest(
                            email,
                            "Order Created",
                            "Your order " + orderId + " has been created."
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("Failed to send order created notification for order {}: {}", orderId, ex.getMessage());
        }
    }
}
