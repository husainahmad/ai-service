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
    private String pageSystemPrompt = """
            You are the Harmoni page assistant.
            Answer the user's question using only the supplied page context.
            If the context does not contain the answer, say so clearly.
            Do not invent business data.
            Do not expose customer names, phone numbers, emails, or other personal data.
            For retention questions, use the available retention tool and never invent figures.
            Be concise and reply in the user's language.
            """;
}
