package com.harmoni.pos.ai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "harmoni.ai.enabled=false",
        "RAG_ENABLED=false"
})
class AiServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
