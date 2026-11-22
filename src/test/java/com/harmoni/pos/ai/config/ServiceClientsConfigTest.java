package com.harmoni.pos.ai.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceClientsConfigTest {

    @Test
    void menuAndOrderClientsCarryInternalAuthHeaders() {
        MenuServiceProperties menu = new MenuServiceProperties();
        menu.setBaseUrl("http://localhost:8082");
        OrderServiceProperties order = new OrderServiceProperties();
        order.setBaseUrl("http://localhost:8083");

        RestClient.Builder builder = RestClient.builder();
        RestClient menuClient = new ServiceClientsConfig().menuRestClient(menu, builder);
        RestClient orderClient = new ServiceClientsConfig().orderRestClient(order, builder);

        assertThat(menuClient).isNotNull();
        assertThat(orderClient).isNotNull();
        assertThat(menu.getUsername()).isEqualTo("Kasir1");
        assertThat(menu.getPermissions()).isEqualTo("menu:view");
        assertThat(order.getPermissions()).isEqualTo("order:view");
    }

    @Test
    void menuPathsUseRealMenuServiceEndpoints() {
        MenuServiceProperties properties = new MenuServiceProperties();
        assertThat(properties.getEndpoints().getCategoryByBrand()).isEqualTo("/api/v1/category/brand/{brandId}");
        assertThat(properties.getEndpoints().getProductByCategoryPrice())
                .isEqualTo("/api/v1/product/category/{id}/price");
        assertThat(properties.getEndpoints().getProductCustomization())
                .isEqualTo("/api/v1/product/{id}/customization");
    }
}
