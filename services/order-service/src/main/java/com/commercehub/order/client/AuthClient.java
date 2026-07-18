package com.commercehub.order.client;

import com.commercehub.order.config.AuthServiceProperties;
import com.commercehub.order.dto.InternalUserResponse;
import com.commercehub.order.exception.NotFoundException;
import com.commercehub.order.exception.ServiceUnavailableException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class AuthClient {

    private final RestClient restClient;
    private final AuthServiceProperties properties;

    public AuthClient(RestClient restClient, AuthServiceProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public String getUserEmail(UUID userId) {
        try {
            InternalUserResponse user = restClient.get()
                    .uri(properties.getBaseUrl() + "/internal/users/{userId}", userId)
                    .retrieve()
                    .body(InternalUserResponse.class);
            if (user == null || user.email() == null) {
                throw new NotFoundException("User not found");
            }
            return user.email();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new NotFoundException("User not found");
        } catch (RestClientException ex) {
            throw new ServiceUnavailableException("Auth service is unavailable");
        }
    }
}
