package com.commercehub.order.integration;

import com.commercehub.messaging.DomainEventPublisher;
import com.commercehub.messaging.MessagingTopology;
import com.commercehub.order.client.AuthClient;
import com.commercehub.order.client.ProductClient;
import com.commercehub.order.dto.CreateOrderRequest;
import com.commercehub.order.dto.ProductSnapshotResponse;
import com.commercehub.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class OrderIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("order_db")
            .withUsername("order_user")
            .withPassword("order_pass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;

    @MockBean
    private ProductClient productClient;

    @MockBean
    private AuthClient authClient;

    @MockBean
    private DomainEventPublisher domainEventPublisher;

    private UUID userId;
    private UUID otherUserId;
    private String userToken;
    private String otherUserToken;
    private String adminToken;

    private static final UUID PRODUCT_ID = UUID.fromString("a1000000-0000-4000-8000-000000000001");

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        userToken = jwtService.generateAccessToken(userId, List.of("USER"));
        otherUserToken = jwtService.generateAccessToken(otherUserId, List.of("USER"));
        adminToken = jwtService.generateAccessToken(UUID.randomUUID(), List.of("ADMIN"));

        when(productClient.getProductSnapshot(PRODUCT_ID))
                .thenReturn(new ProductSnapshotResponse(PRODUCT_ID, "Gaming Mouse", new BigDecimal("799.99")));
        when(authClient.getUserEmail(any(UUID.class))).thenReturn("user@example.com");
        doNothing().when(domainEventPublisher).publish(any(), any());
    }

    /**
     * Guards the queues this service consumes. A listener bean that drops out of
     * the context leaves its queue without a consumer, which kills the
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
                MessagingTopology.QUEUE_ORDER_STOCK_RESERVED,
                MessagingTopology.QUEUE_ORDER_PAYMENT_SUCCEEDED,
                MessagingTopology.QUEUE_ORDER_PAYMENT_FAILED);
    }

    @Test
    void createOrder_withoutToken_returns401() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new CreateOrderRequest.OrderItemRequest(PRODUCT_ID, 1))
        );

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createOrder_withMalformedProductId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"productId":"not-a-uuid","quantity":1}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void createGetListAndCancelOrder_flow() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new CreateOrderRequest.OrderItemRequest(PRODUCT_ID, 2))
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalPrice").value(1599.98))
                .andExpect(jsonPath("$.orderId").exists())
                .andReturn();

        String orderId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("orderId")
                .asText();

        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.items[0].productName").value("Gaming Mouse"))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        mockMvc.perform(get("/api/v1/orders/user/" + userId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(orderId))
                .andExpect(jsonPath("$[0].status").value("CREATED"));

        mockMvc.perform(delete("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void getOrder_otherUser_returns403() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new CreateOrderRequest.OrderItemRequest(PRODUCT_ID, 1))
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String orderId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("orderId")
                .asText();

        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId));
    }

    @Test
    void listOrders_otherUser_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/orders/user/" + userId)
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isForbidden());
    }
}
