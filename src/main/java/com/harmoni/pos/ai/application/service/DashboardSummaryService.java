package com.harmoni.pos.ai.application.service;

import com.harmoni.pos.ai.application.port.in.AnswerDashboardSummaryCommand;
import com.harmoni.pos.ai.application.port.in.AnswerDashboardSummaryUseCase;
import com.harmoni.pos.ai.application.port.out.LlmPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardSummaryService implements AnswerDashboardSummaryUseCase {

    private static final String FALLBACK = "I could not generate a reply. Please try again.";

    private final LlmPort llm;

    @Override
    public String answer(AnswerDashboardSummaryCommand command) {
        String page = command.page() == null || command.page().isBlank() ? "Dashboard" : command.page();
        String prompt = """
                You are the Harmoni page analyst.
                Answer the user's question using the context below.
                Current page: %s
                The context may cover the dashboard, reports, categories, products, promotions,
                customers, or other pages in the application.
                If the context does not contain the answer, say so clearly and do not invent data.
                You have at most one related MCP tool available. Use it only when the page context
                is insufficient for the question.
                Do not expose customer names, phone numbers, or other personal data.
                Format the answer as Markdown with short paragraphs, bullet lists and bold key figures.
                Use a table when comparing values. Do not wrap the whole answer in a code fence.
                Page context:
                %s

                User question:
                %s
                """.formatted(page, command.summary(), command.message());
        String reply = llm.generateWithoutMemory(prompt, command.tool());
        return reply == null || reply.isBlank() ? FALLBACK : reply;
    }
}
