package com.harmoni.pos.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "harmoni.services.menu")
public class MenuServiceProperties {

    private String baseUrl = "http://localhost:8082";
    private Integer brandId = 1;

    /** Internal service identity forwarded to the menu service for authorization. */
    private String username = "Kasir1";
    private String permissions = "menu:view";

    private Endpoints endpoints = new Endpoints();

    @Data
    public static class Endpoints {
        private String productSearch = "/api/v1/product/search";
        private String productById = "/api/v1/product/{id}";
        private String productCustomization = "/api/v1/product/{id}/customization";
        private String categorySearch = "/api/v1/category/search";
        private String categoryByBrand = "/api/v1/category/brand/{brandId}";
        private String productByCategoryPrice = "/api/v1/product/category/{id}/price";
        private String productByCategoryPaged = "/api/v1/product/category/{categoryId}/{brandId}";
    }
}
