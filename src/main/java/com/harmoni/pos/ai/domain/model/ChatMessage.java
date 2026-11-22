package com.harmoni.pos.ai.domain.model;

import java.time.Instant;

public record ChatMessage(Long id, Long sessionId, String role, String content, Instant createdAt) {

    public static ChatMessage user(long sessionId, String content) {
        return new ChatMessage(null, sessionId, "USER", content, null);
    }

    public static ChatMessage assistant(long sessionId, String content) {
        return new ChatMessage(null, sessionId, "ASSISTANT", content, null);
    }
}
