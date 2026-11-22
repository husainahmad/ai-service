package com.harmoni.pos.ai.application.port.in;


public record StreamChatMessageCommand(long sessionId, String message) {
}
