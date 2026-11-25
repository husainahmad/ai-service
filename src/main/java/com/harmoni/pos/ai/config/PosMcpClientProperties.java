package com.harmoni.pos.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Data
@ConfigurationProperties(prefix = "harmoni.mcp.client")
public class PosMcpClientProperties {

    private String url = "http://localhost:8087";
    private String sseEndpoint = "/api/v1/mcp/sse";
    private boolean enabled = true;
    private String token;
    private String username = "Kasir1";
    private String roles;
    private String permissions = "order:view,report:view,menu:view,menu:edit,organization:read,customer:read";
    private String scopes;
    private Duration requestTimeout = Duration.ofSeconds(30);
}
