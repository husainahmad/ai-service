package com.harmoni.pos.ai.adapter.in.web;

import com.harmoni.pos.ai.adapter.in.web.dto.AiChatRequest;
import com.harmoni.pos.ai.adapter.in.web.dto.ChatMessageResponse;
import com.harmoni.pos.ai.application.port.in.SendChatMessageCommand;
import com.harmoni.pos.ai.application.port.in.SendChatMessageUseCase;
import com.harmoni.pos.ai.application.port.in.StreamChatMessageCommand;
import com.harmoni.pos.ai.application.port.in.StreamChatMessageUseCase;
import com.harmoni.pos.ai.domain.model.ChatMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/customer-sessions")
@RequiredArgsConstructor
@Tag(name = "AI Chat", description = "AI ordering assistant chat")
public class AiChatController {

    private final SendChatMessageUseCase sendChatMessageUseCase;
    private final StreamChatMessageUseCase streamChatMessageUseCase;

    @Operation(summary = "Send a message to the AI assistant")
    @PostMapping("/{sessionId}/chat")
    public ChatMessageResponse chat(@PathVariable long sessionId, @Valid @RequestBody AiChatRequest request) {
        return toResponse(sendChatMessageUseCase.send(new SendChatMessageCommand(sessionId, request.message())));
    }

    @Operation(summary = "Alias of /chat")
    @PostMapping("/{sessionId}/messages/ai")
    public ChatMessageResponse chatAlias(@PathVariable long sessionId, @Valid @RequestBody AiChatRequest request) {
        return chat(sessionId, request);
    }

    @Operation(summary = "Stream the assistant reply as Server-Sent Events")
    @PostMapping(value = "/{sessionId}/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@PathVariable long sessionId, @Valid @RequestBody AiChatRequest request) {
        return streamChatMessageUseCase.stream(new StreamChatMessageCommand(sessionId, request.message()));
    }

    @Operation(summary = "Alias of /chat/stream")
    @PostMapping(value = "/{sessionId}/messages/ai/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStreamAlias(@PathVariable long sessionId, @Valid @RequestBody AiChatRequest request) {
        return chatStream(sessionId, request);
    }

    private static ChatMessageResponse toResponse(ChatMessage message) {
        return new ChatMessageResponse(message.id(), message.sessionId(), message.role(),
                message.content(), message.createdAt());
    }
}
