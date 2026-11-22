package com.harmoni.pos.ai.config;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PosMcpClientConfig {

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "harmoni.mcp.client", name = "enabled", havingValue = "true",
            matchIfMissing = true)
    public McpSyncClient posMcpClient(PosMcpClientProperties properties) {
        HttpClientSseClientTransport transport = HttpClientSseClientTransport
                .builder(properties.getUrl())
                .sseEndpoint(properties.getSseEndpoint())
                .customizeRequest(builder -> {
                    if (properties.getToken() != null && !properties.getToken().isBlank()) {
                        builder.header("Authorization", "Bearer " + properties.getToken().trim());
                    } else {
                        builder.header("X-Username", properties.getUsername());
                        header(builder, "X-Roles", properties.getRoles());
                        header(builder, "X-Permissions", properties.getPermissions());
                        header(builder, "X-Scopes", properties.getScopes());
                    }
                })
                .build();
        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(properties.getRequestTimeout())
                .build();
        client.initialize();
        return client;
    }

    @Bean
    @ConditionalOnProperty(prefix = "harmoni.mcp.client", name = "enabled", havingValue = "true",
            matchIfMissing = true)
    public ToolCallbackProvider posMcpTools(McpSyncClient posMcpClient) {
        return new SyncMcpToolCallbackProvider(posMcpClient);
    }

    private void header(java.net.http.HttpRequest.Builder builder, String name, String value) {
        if (value != null && !value.isBlank()) {
            builder.header(name, value);
        }
    }
}
