package com.harmoni.pos.ai.application.service;

import com.harmoni.pos.ai.application.port.in.SendChatMessageCommand;
import com.harmoni.pos.ai.application.port.in.StreamChatMessageCommand;
import com.harmoni.pos.ai.application.port.out.AiSessionPort;
import com.harmoni.pos.ai.application.port.out.LlmPort;
import com.harmoni.pos.ai.domain.exception.SessionNotFoundException;
import com.harmoni.pos.ai.domain.model.AiSession;
import com.harmoni.pos.ai.domain.model.ChatMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatServiceTest {

    @Mock
    private AiSessionPort sessions;

    @Mock
    private LlmPort llm;

    @InjectMocks
    private AiChatService service;

    @Test
    void send_persistsUserAndAssistantMessages() {
        when(sessions.findById(100L)).thenReturn(Optional.of(new AiSession(100L, 7L, "OPEN")));
        when(llm.generate(100L, "halo")).thenReturn("Hello!");

        ChatMessage reply = service.send(new SendChatMessageCommand(100L, "halo"));

        assertThat(reply.role()).isEqualTo("ASSISTANT");
        assertThat(reply.content()).isEqualTo("Hello!");
        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(sessions, times(2)).saveMessage(eq(100L), captor.capture());
        assertThat(captor.getAllValues().get(0).role()).isEqualTo("USER");
        assertThat(captor.getAllValues().get(1).role()).isEqualTo("ASSISTANT");
    }

    @Test
    void send_missingSession_throwsWithoutCallingLlm() {
        when(sessions.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.send(new SendChatMessageCommand(100L, "halo")))
                .isInstanceOf(SessionNotFoundException.class);
        verify(llm, never()).generate(anyLong(), any());
    }

    @Test
    void send_closedSession_throwsWithoutCallingLlm() {
        when(sessions.findById(100L)).thenReturn(Optional.of(new AiSession(100L, 7L, "CLOSED")));

        assertThatThrownBy(() -> service.send(new SendChatMessageCommand(100L, "halo")))
                .isInstanceOf(IllegalStateException.class);
        verify(llm, never()).generate(anyLong(), any());
    }

    @Test
    void stream_persistsAssistantMessageOnCompletion() {
        when(sessions.findById(100L)).thenReturn(Optional.of(new AiSession(100L, 7L, "OPEN")));
        when(llm.stream(100L, "halo")).thenReturn(Flux.just("Hello", "!"));

        // Streamed chunks carry one extra leading space so SSE clients that strip
        // the space after "data:" receive the original token intact.
        StepVerifier.create(service.stream(new StreamChatMessageCommand(100L, "halo")))
                .expectNext(" Hello", " !")
                .verifyComplete();

        // The persisted assistant message keeps the original, unprefixed text.
        verify(sessions).saveMessage(eq(100L),
                argThat(message -> message.role().equals("ASSISTANT") && message.content().equals("Hello!")));
    }
}
