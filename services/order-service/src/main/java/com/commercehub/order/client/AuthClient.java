package com.commercehub.order.client;

import com.commercehub.order.config.AuthServiceProperties;
import com.commercehub.order.dto.InternalUserResponse;
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
public class AuthClient {

    private final RestClient restClient;
    private final AuthServiceProperties properties;

    public AuthClient(RestClient restClient, AuthServiceProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @CircuitBreaker(name = "orderAuth")
    @Retry(name = "orderAuth")
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
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new NotFoundException("User not found");
            }
            throw new ServiceUnavailableException("Auth service is unavailable");
        } catch (HttpServerErrorException ex) {
            if (isTransient(ex.getStatusCode())) {
                throw new TransientServiceUnavailableException("Auth service is temporarily unavailable");
            }
            throw new ServiceUnavailableException("Auth service is unavailable");
        } catch (ResourceAccessException ex) {
            throw new TransientServiceUnavailableException("Auth service is temporarily unavailable");
        } catch (RestClientException ex) {
            throw new ServiceUnavailableException("Auth service is unavailable");
        }
    }

    private boolean isTransient(HttpStatusCode statusCode) {
        int status = statusCode.value();
        return status == 502 || status == 503 || status == 504;
    }
}
