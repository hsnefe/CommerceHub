package com.commercehub.inventory.integration;

import com.commercehub.inventory.client.ProductClient;
import com.commercehub.inventory.dto.InventoryRequest;
import com.commercehub.inventory.dto.InventoryUpdateRequest;
import com.commercehub.inventory.dto.QuantityAdjustmentRequest;
import com.commercehub.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class InventoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("inventory_db")
            .withUsername("inventory_user")
            .withPassword("inventory_pass");

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

    @MockBean
    private ProductClient productClient;

    private String adminToken;
    private String userToken;

    private static final UUID SEED_PRODUCT_ID = UUID.fromString("a1000000-0000-4000-8000-000000000001");
    private static final UUID LOW_STOCK_PRODUCT_ID = UUID.fromString("a1000000-0000-4000-8000-000000000002");

    @BeforeEach
    void setUp() {
        adminToken = jwtService.generateAccessToken(UUID.randomUUID(), List.of("ADMIN"));
        userToken = jwtService.generateAccessToken(UUID.randomUUID(), List.of("USER"));
        doNothing().when(productClient).validateProductExists(any());
    }

    @Test
    void listInventory_publicAccess() throws Exception {
        mockMvc.perform(get("/api/v1/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void getInventory_seedData() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/" + SEED_PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(SEED_PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.availableQuantity").value(100))
                .andExpect(jsonPath("$.lowStock").value(false));
    }

    @Test
    void getInventory_lowStockFlag() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/" + LOW_STOCK_PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(3))
                .andExpect(jsonPath("$.lowStock").value(true));
    }

    @Test
    void createInventory_withoutToken_returns401() throws Exception {
        InventoryRequest request = new InventoryRequest(UUID.randomUUID(), 50, 5);

        mockMvc.perform(post("/api/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createInventory_withMalformedProductId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/inventory")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"not-a-uuid","availableQuantity":50,"lowStockThreshold":5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void createInventory_asUser_returns403() throws Exception {
        InventoryRequest request = new InventoryRequest(UUID.randomUUID(), 50, 5);

        mockMvc.perform(post("/api/v1/inventory")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void inventoryCrud_andInternalAdjustments() throws Exception {
        UUID newProductId = UUID.fromString("b2000000-0000-4000-8000-000000000099");
        InventoryRequest createRequest = new InventoryRequest(newProductId, 25, 5);

        mockMvc.perform(post("/api/v1/inventory")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.availableQuantity").value(25))
                .andExpect(jsonPath("$.lowStock").value(false));

        mockMvc.perform(patch("/api/v1/inventory/" + newProductId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InventoryUpdateRequest(30, 10))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(30))
                .andExpect(jsonPath("$.lowStockThreshold").value(10));

        mockMvc.perform(post("/internal/inventory/" + newProductId + "/decrement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new QuantityAdjustmentRequest(5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(25));

        mockMvc.perform(post("/internal/inventory/" + newProductId + "/increment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new QuantityAdjustmentRequest(3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(28));
    }

    @Test
    void decrement_insufficientStock_returns409() throws Exception {
        UUID outOfStockId = UUID.fromString("a1000000-0000-4000-8000-000000000003");

        mockMvc.perform(post("/internal/inventory/" + outOfStockId + "/decrement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new QuantityAdjustmentRequest(1))))
                .andExpect(status().isConflict());
    }
}
