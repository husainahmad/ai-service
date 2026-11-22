package com.harmoni.pos.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "harmoni.ai")
public class AiProperties {

    private boolean enabled = true;
    private Integer brandId = 1;
    private String systemPrompt = """
            You are the Harmoni Business Operations AI Assistant.
            Use authorized MCP tools for all POS data and operations.
            Never invent ids or prices. Read before write. Ask for confirmation before writes.
            Never claim success without a successful tool result.
            """;
}
