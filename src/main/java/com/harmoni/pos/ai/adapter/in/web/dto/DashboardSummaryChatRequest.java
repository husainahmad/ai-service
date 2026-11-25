package com.harmoni.pos.ai.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DashboardSummaryChatRequest(
        @NotBlank @Size(max = 2000) String message,
        @Size(max = 200) String page,
        @Size(max = 100) String tool,
        @NotBlank @Size(max = 50000) String summary) {
}
