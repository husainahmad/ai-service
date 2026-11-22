package com.harmoni.pos.ai.adapter.in.web.dto;

import java.time.Instant;

public record ChatMessageResponse(Long id, Long sessionId, String role, String message, Instant createdAt) {
}
