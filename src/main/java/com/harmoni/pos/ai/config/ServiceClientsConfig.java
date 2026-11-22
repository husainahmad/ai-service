package com.harmoni.pos.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;


/** RestClient beans for the internal menu, order and customer services. */
@Configuration
public class ServiceClientsConfig {

    @Bean
    public RestClient menuRestClient(MenuServiceProperties properties, RestClient.Builder builder) {
        return builder.baseUrl(properties.getBaseUrl())
                .defaultHeaders(headers -> {
                    headers.set("X-Username", properties.getUsername());
                    headers.set("X-Permissions", properties.getPermissions());
                })
                .build();
    }

    @Bean
    public RestClient orderRestClient(OrderServiceProperties properties, RestClient.Builder builder) {
        return builder.baseUrl(properties.getBaseUrl())
                .defaultHeaders(headers -> {
                    headers.set("X-Username", properties.getDefaultUsername());
                    headers.set("X-Permissions", properties.getPermissions());
                })
                .build();
    }

    @Bean
    public RestClient customerRestClient(CustomerServiceProperties properties, RestClient.Builder builder) {
        return builder.baseUrl(properties.getBaseUrl()).build();
    }
}
