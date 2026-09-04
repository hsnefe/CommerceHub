package com.commercehub.notification.integration;

import com.commercehub.messaging.MessagingTopology;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;

    /**
     * Tests here have to sit in a @Nested group like the rest of this class:
     * Surefire reports the outer class of a @Nested hierarchy separately and
     * does not execute methods declared directly on it.
     */
    @Nested
    class Messaging {

        /**
         * Guards the queues this service consumes. A listener bean that drops out
         * of the context leaves its queue without a consumer, which kills the
         * asynchronous flow while every other test still passes.
         */
        @Test
        void registersAListenerForEveryQueueItConsumes() {
            Set<String> queues = rabbitListenerEndpointRegistry.getListenerContainers().stream()
                    .filter(AbstractMessageListenerContainer.class::isInstance)
                    .map(AbstractMessageListenerContainer.class::cast)
                    .flatMap(container -> Arrays.stream(container.getQueueNames()))
                    .collect(Collectors.toSet());

            assertThat(queues).containsExactlyInAnyOrder(
                    MessagingTopology.QUEUE_NOTIFICATION_ORDER_EVENTS,
                    MessagingTopology.QUEUE_NOTIFICATION_STOCK_EVENTS,
                    MessagingTopology.QUEUE_NOTIFICATION_PAYMENT_EVENTS);
        }
    }

    private Map<String, String> validRequest() {
        Map<String, String> request = new LinkedHashMap<>();
        request.put("email", "user@example.com");
        request.put("subject", "Order Created");
        request.put("message", "Your order has been created.");
        return request;
    }

    @Nested
    class SendNotification {

        @Test
        void sendNotification_success() throws Exception {
            mockMvc.perform(post("/api/v1/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        void sendNotification_orderCancelledScenario() throws Exception {
            Map<String, String> request = validRequest();
            request.put("subject", "Order Cancelled");
            request.put("message", "Your order has been cancelled.");

            mockMvc.perform(post("/api/v1/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        void sendNotification_alternateEmailFormat() throws Exception {
            Map<String, String> request = validRequest();
            request.put("email", "name.surname+tag@sub.domain.co");

            mockMvc.perform(post("/api/v1/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    class Validation {

        @Test
        void sendNotification_invalidEmail_returnsBadRequest() throws Exception {
            Map<String, String> request = validRequest();
            request.put("email", "not-an-email");

            mockMvc.perform(post("/api/v1/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.path").value("/api/v1/notifications"));
        }

        @Test
        void sendNotification_blankEmail_returnsBadRequest() throws Exception {
            Map<String, String> request = validRequest();
            request.put("email", "   ");

            mockMvc.perform(post("/api/v1/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
        }

        @Test
        void sendNotification_missingEmail_returnsBadRequest() throws Exception {
            Map<String, String> request = validRequest();
            request.remove("email");

            mockMvc.perform(post("/api/v1/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
        }

        @Test
        void sendNotification_blankSubject_returnsBadRequest() throws Exception {
            Map<String, String> request = validRequest();
            request.put("subject", "");

            mockMvc.perform(post("/api/v1/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
        }

        @Test
        void sendNotification_missingSubject_returnsBadRequest() throws Exception {
            Map<String, String> request = validRequest();
            request.remove("subject");

            mockMvc.perform(post("/api/v1/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
        }

        @Test
        void sendNotification_blankMessage_returnsBadRequest() throws Exception {
            Map<String, String> request = validRequest();
            request.put("message", "");

            mockMvc.perform(post("/api/v1/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
        }

        @Test
        void sendNotification_missingMessage_returnsBadRequest() throws Exception {
            Map<String, String> request = validRequest();
            request.remove("message");

            mockMvc.perform(post("/api/v1/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
        }

        @Test
        void sendNotification_emptyBody_returnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
        }

        @Test
        void sendNotification_malformedJson_returnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid-json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.message").value("Malformed request body"));
        }
    }

    @Nested
    class SecurityAndDocs {

        @Test
        void swaggerUi_isPubliclyAccessible() throws Exception {
            mockMvc.perform(get("/swagger-ui.html"))
                    .andExpect(status().is3xxRedirection());
        }

        @Test
        void apiDocs_isPubliclyAccessible() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.info.title").value("CommerceHub Notification Service API"));
        }

        @Test
        void unknownProtectedEndpoint_requiresAuthentication() throws Exception {
            mockMvc.perform(get("/api/v1/notifications"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        }
    }
}
