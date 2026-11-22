package com.harmoni.pos.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "harmoni.ai")
public class AiProperties {

    private boolean enabled = true;
    private boolean ragEnabled = false;
    private Integer brandId = 1;
    private String systemPrompt = "You are Harmoni AI, a friendly restaurant ordering assistant.";
}
