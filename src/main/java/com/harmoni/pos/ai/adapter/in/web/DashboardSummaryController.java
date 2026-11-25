package com.harmoni.pos.ai.adapter.in.web;

import com.harmoni.pos.ai.adapter.in.web.dto.DashboardSummaryChatRequest;
import com.harmoni.pos.ai.adapter.in.web.dto.DashboardSummaryChatResponse;
import com.harmoni.pos.ai.application.port.in.AnswerDashboardSummaryCommand;
import com.harmoni.pos.ai.application.port.in.AnswerDashboardSummaryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/dashboard")
@RequiredArgsConstructor
@Tag(name = "Page AI", description = "AI answers about the current dashboard or page context")
public class DashboardSummaryController {

    private final AnswerDashboardSummaryUseCase answerDashboardSummaryUseCase;

    @Operation(summary = "Answer a question about the current page context")
    @PostMapping("/summary-chat")
    public DashboardSummaryChatResponse summaryChat(@Valid @RequestBody DashboardSummaryChatRequest request) {
        return new DashboardSummaryChatResponse(answerDashboardSummaryUseCase.answer(
                new AnswerDashboardSummaryCommand(request.message(), request.page(),
                        request.tool(), request.summary())));
    }
}
