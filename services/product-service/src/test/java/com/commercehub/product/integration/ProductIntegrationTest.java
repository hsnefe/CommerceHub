package com.commercehub.product.integration;

import com.commercehub.product.dto.CategoryRequest;
import com.commercehub.product.dto.ProductRequest;
import com.commercehub.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class ProductIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("product_db")
            .withUsername("product_user")
            .withPassword("product_pass");

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

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUpTokens() {
        adminToken = jwtService.generateAccessToken(UUID.randomUUID(), List.of("ADMIN"));
        userToken = jwtService.generateAccessToken(UUID.randomUUID(), List.of("USER"));
    }

    @Test
    void listProducts_publicAccess() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void createProduct_withoutToken_returns401() throws Exception {
        ProductRequest request = new ProductRequest(
                "Mouse",
                "Desc",
                new BigDecimal("100.00"),
                UUID.randomUUID()
        );

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProduct_withMalformedCategoryId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Mouse","description":"Desc","price":100.00,"categoryId":"not-a-uuid"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void createProduct_asUser_returns403() throws Exception {
        ProductRequest request = new ProductRequest(
                "Mouse",
                "Desc",
                new BigDecimal("100.00"),
                UUID.randomUUID()
        );

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void productCrud_andInternalSnapshot() throws Exception {
        MvcResult categoryResult = mockMvc.perform(post("/api/v1/products/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRequest("Electronics"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Electronics"))
                .andReturn();

        UUID categoryId = UUID.fromString(
                objectMapper.readTree(categoryResult.getResponse().getContentAsString()).get("id").asText()
        );

        ProductRequest productRequest = new ProductRequest(
                "Gaming Mouse",
                "Wireless gaming mouse",
                new BigDecimal("799.99"),
                categoryId
        );

        MvcResult productResult = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Gaming Mouse"))
                .andExpect(jsonPath("$.price").value(799.99))
                .andReturn();

        UUID productId = UUID.fromString(
                objectMapper.readTree(productResult.getResponse().getContentAsString()).get("id").asText()
        );

        mockMvc.perform(get("/api/v1/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Wireless gaming mouse"))
                .andExpect(jsonPath("$.currency").value("TRY"))
                .andExpect(jsonPath("$.categoryId").value(categoryId.toString()));

        mockMvc.perform(get("/api/v1/products")
                        .param("categoryId", categoryId.toString())
                        .param("name", "gaming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Gaming Mouse"));

        mockMvc.perform(get("/internal/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(799.99))
                .andExpect(jsonPath("$.name").value("Gaming Mouse"));

        ProductRequest updateRequest = new ProductRequest(
                "Gaming Mouse V2",
                "Updated",
                new BigDecimal("899.99"),
                categoryId
        );

        mockMvc.perform(put("/api/v1/products/" + productId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gaming Mouse V2"));

        mockMvc.perform(delete("/api/v1/products/" + productId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/products/" + productId))
                .andExpect(status().isNotFound());
    }
}
