package com.harmoni.pos.ai.config;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
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
        return client;
    }

    @Bean
    @ConditionalOnProperty(prefix = "harmoni.mcp.client", name = "enabled", havingValue = "true",
            matchIfMissing = true)
    public ToolCallbackProvider posMcpTools(McpSyncClient posMcpClient, PosMcpClientProperties properties) {
        if (properties.getToken() == null || properties.getToken().isBlank()) {
            log.info("MCP client connecting directly to {} without a JWT", properties.getUrl());
        }
        try {
            posMcpClient.initialize();
            log.info("MCP client connected to {}; tools are available", properties.getUrl());
            return new SyncMcpToolCallbackProvider(posMcpClient);
        } catch (RuntimeException e) {
            String message = rootMessage(e);
            if (properties.getToken() == null || properties.getToken().isBlank()) {
                log.warn("MCP server is unavailable at {} ({}). Direct mode does not use a JWT; "
                                + "make sure mcp-pos-server is running on port 8087. "
                                + "AI will run without POS tools.",
                        properties.getUrl(), message);
            } else {
                log.warn("MCP server is unavailable at {} ({}). Check the gateway and JWT, "
                                + "or unset MCP_POS_TOKEN to connect directly. "
                                + "AI will run without POS tools.",
                        properties.getUrl(), message);
            }
            return ToolCallbackProvider.from(List.of());
        }
    }

    private static String rootMessage(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
    }

    private void header(java.net.http.HttpRequest.Builder builder, String name, String value) {
        if (value != null && !value.isBlank()) {
            builder.header(name, value);
        }
    }
}
