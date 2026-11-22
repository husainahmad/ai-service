package com.harmoni.pos.ai.application.service;

import com.harmoni.pos.ai.application.port.in.SendChatMessageCommand;
import com.harmoni.pos.ai.application.port.in.SendChatMessageUseCase;
import com.harmoni.pos.ai.application.port.in.StreamChatMessageCommand;
import com.harmoni.pos.ai.application.port.in.StreamChatMessageUseCase;
import com.harmoni.pos.ai.application.port.out.AiSessionPort;
import com.harmoni.pos.ai.application.port.out.LlmPort;
import com.harmoni.pos.ai.domain.exception.SessionNotFoundException;
import com.harmoni.pos.ai.domain.model.AiSession;
import com.harmoni.pos.ai.domain.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AiChatService implements SendChatMessageUseCase, StreamChatMessageUseCase {

    private static final String FALLBACK = "I could not generate a reply. Please try again.";

    private final AiSessionPort sessions;
    private final LlmPort llm;

    @Override
    public ChatMessage send(SendChatMessageCommand command) {
        AiSession session = requireOpenSession(command.sessionId());
        sessions.saveMessage(command.sessionId(), ChatMessage.user(command.sessionId(), command.message()));

        String reply = llm.generate(command.sessionId(), command.message());
        if (reply == null || reply.isBlank()) {
            reply = FALLBACK;
        }
        ChatMessage assistantMessage = new ChatMessage(null, command.sessionId(), "ASSISTANT",
                reply, Instant.now());
        sessions.saveMessage(command.sessionId(), assistantMessage);
        return assistantMessage;
    }

    @Override
    public Flux<String> stream(StreamChatMessageCommand command) {
        requireOpenSession(command.sessionId());
        sessions.saveMessage(command.sessionId(), ChatMessage.user(command.sessionId(), command.message()));

        StringBuilder reply = new StringBuilder();
        return llm.stream(command.sessionId(), command.message())
                .doOnNext(reply::append)
                // SSE clients drop the single space after "data:". Emit an extra
                // leading space so the original token (and its word separators)
                // reaches the browser intact.
                .map(chunk -> " " + chunk)
                .doOnComplete(() -> sessions.saveMessage(command.sessionId(),
                        ChatMessage.assistant(command.sessionId(), reply.toString())));
    }

    private AiSession requireOpenSession(long sessionId) {
        AiSession session = sessions.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
        if (!session.isOpen()) {
            throw new IllegalStateException("Customer session " + sessionId + " is closed");
        }
        return session;
    }
}
