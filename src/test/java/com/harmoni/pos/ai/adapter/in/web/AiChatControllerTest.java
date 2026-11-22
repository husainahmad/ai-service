package com.harmoni.pos.ai.adapter.in.web;

import com.harmoni.pos.ai.adapter.in.web.dto.ChatMessageResponse;
import com.harmoni.pos.ai.application.port.in.SendChatMessageUseCase;
import com.harmoni.pos.ai.application.port.in.StreamChatMessageUseCase;
import com.harmoni.pos.ai.domain.model.ChatMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiChatController.class)
class AiChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SendChatMessageUseCase sendChatMessageUseCase;

    @MockitoBean
    private StreamChatMessageUseCase streamChatMessageUseCase;

    @Test
    void chat_returnsAssistantReply() throws Exception {
        when(sendChatMessageUseCase.send(any())).thenReturn(
                new ChatMessage(9L, 100L, "ASSISTANT", "Hello!", Instant.now()));

        mockMvc.perform(post("/api/v1/customer-sessions/100/chat")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"halo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ASSISTANT"))
                .andExpect(jsonPath("$.message").value("Hello!"));
    }

    @Test
    void chatStream_returnsSseChunks() throws Exception {
        when(streamChatMessageUseCase.stream(any())).thenReturn(Flux.just("Hello", " world"));

        mockMvc.perform(post("/api/v1/customer-sessions/100/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"halo\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data:Hello")));
    }

    @Test
    void chat_blankMessage_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/customer-sessions/100/chat")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
