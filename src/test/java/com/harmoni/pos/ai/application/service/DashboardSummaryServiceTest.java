package com.harmoni.pos.ai.application.service;

import com.harmoni.pos.ai.application.port.in.AnswerDashboardSummaryCommand;
import com.harmoni.pos.ai.application.port.out.LlmPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardSummaryServiceTest {

    @Mock
    private LlmPort llm;

    @InjectMocks
    private DashboardSummaryService service;

    @Test
    void answer_includesSummaryAndQuestionInPrompt() {
        when(llm.generateWithoutMemory(anyString(), anyString()))
                .thenReturn("Sales are lower than yesterday.");

        String answer = service.answer(new AnswerDashboardSummaryCommand(
                "Why are sales lower?", "Dashboard", "pos_get_sales_report",
                "Sales: Rp 1.000.000\nTransactions: 20"));

        assertThat(answer).isEqualTo("Sales are lower than yesterday.");
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(llm).generateWithoutMemory(prompt.capture(), eq("pos_get_sales_report"));
        assertThat(prompt.getValue())
                .contains("Sales: Rp 1.000.000")
                .contains("Transactions: 20")
                .contains("Current page: Dashboard")
                .contains("Page context")
                .contains("reports, categories, products, promotions")
                .contains("Do not expose customer names")
                .contains("Format the answer as Markdown")
                .contains("Why are sales lower?");
    }

    @Test
    void answer_blankReply_returnsFallback() {
        when(llm.generateWithoutMemory(anyString(), anyString())).thenReturn(" ");

        String answer = service.answer(new AnswerDashboardSummaryCommand(
                "Any insight?", "Category", "pos_search_categories", "Sales: 0"));

        assertThat(answer).isEqualTo("I could not generate a reply. Please try again.");
    }
}
