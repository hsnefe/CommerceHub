package com.commercehub.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "commercehub.inventory-service")
public class InventoryServiceProperties {

    private String baseUrl = "http://localhost:8083";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
