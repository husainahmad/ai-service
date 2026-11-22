package com.harmoni.pos.ai.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AiChatRequest(@NotBlank String message) {
}
