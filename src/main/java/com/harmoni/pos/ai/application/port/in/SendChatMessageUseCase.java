package com.harmoni.pos.ai.application.port.in;

import com.harmoni.pos.ai.domain.model.ChatMessage;

public interface SendChatMessageUseCase {

    ChatMessage send(SendChatMessageCommand command);
}
