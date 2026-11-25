package com.harmoni.pos.ai;

import com.harmoni.pos.ai.config.AiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.ai.openai.api-key=test",
        "harmoni.ai.enabled=false",
        "harmoni.mcp.client.enabled=false",
        "RAG_ENABLED=false"
})
class AiServiceApplicationTests {

    @Autowired
    private AiProperties properties;

    @Test
    void contextLoads() {
    }

    @Test
    void loadsPageSystemPrompt() {
        assertThat(properties.getPageSystemPrompt())
                .contains("Harmoni page assistant")
                .contains("Do not invent business data");
    }
}
