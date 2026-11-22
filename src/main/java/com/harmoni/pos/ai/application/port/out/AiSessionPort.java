package com.harmoni.pos.ai.application.port.out;

import com.harmoni.pos.ai.domain.model.AiSession;
import com.harmoni.pos.ai.domain.model.ChatMessage;

import java.util.Optional;

public interface AiSessionPort {

    Optional<AiSession> findById(long sessionId);

    void saveMessage(long sessionId, ChatMessage message);
}
