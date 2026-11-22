package com.harmoni.pos.ai.application.port.in;

public record SendChatMessageCommand(long sessionId, String message) {
}
