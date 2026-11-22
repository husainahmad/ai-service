package com.harmoni.pos.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "harmoni.services.order")
public class OrderServiceProperties {

    private String baseUrl = "http://localhost:8083";
    private Integer storeId = 1;
    private Integer storeServiceTypesId = 1;
    private String defaultUsername = "Kasir1";

    /** Internal service permissions forwarded to the order service. */
    private String permissions = "order:view";

    private Endpoints endpoints = new Endpoints();

    @Data
    public static class Endpoints {
        private String createOrder = "/api/v1/order";
    }
}
