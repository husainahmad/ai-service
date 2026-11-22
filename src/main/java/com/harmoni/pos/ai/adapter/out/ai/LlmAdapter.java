package com.harmoni.pos.ai.adapter.out.ai;

import com.harmoni.pos.ai.application.port.out.LlmPort;
import com.harmoni.pos.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
public class LlmAdapter implements LlmPort {

    private static final String DISABLED = "AI assistant is currently disabled.";

    private final ChatClient chatClient;
    private final AiProperties properties;

    @Override
    public String generate(long sessionId, String prompt) {
        if (!properties.isEnabled()) {
            return DISABLED;
        }
        String reply = chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(sessionId)))
                .user(prompt)
                .call().content();
        return reply == null || reply.isBlank() ? "I could not generate a reply. Please try again." : reply;
    }

    @Override
    public Flux<String> stream(long sessionId, String prompt) {
        if (!properties.isEnabled()) {
            return Flux.just(DISABLED);
        }
        return chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(sessionId)))
                .user(prompt)
                .stream().content();
    }
}
