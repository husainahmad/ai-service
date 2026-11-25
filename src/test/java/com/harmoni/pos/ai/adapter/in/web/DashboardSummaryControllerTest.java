package com.harmoni.pos.ai.adapter.in.web;

import com.harmoni.pos.ai.application.port.in.AnswerDashboardSummaryCommand;
import com.harmoni.pos.ai.application.port.in.AnswerDashboardSummaryUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardSummaryController.class)
class DashboardSummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnswerDashboardSummaryUseCase answerDashboardSummaryUseCase;

    @Test
    void summaryChat_returnsAnswer() throws Exception {
        when(answerDashboardSummaryUseCase.answer(any())).thenReturn("Sales are down because of fewer orders.");

        mockMvc.perform(post("/api/v1/ai/dashboard/summary-chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Why are sales down?","page":"Dashboard","tool":"pos_get_sales_report","summary":"Sales: Rp 1.000.000"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Sales are down because of fewer orders."));

        ArgumentCaptor<AnswerDashboardSummaryCommand> command =
                ArgumentCaptor.forClass(AnswerDashboardSummaryCommand.class);
        verify(answerDashboardSummaryUseCase).answer(command.capture());
        assertThat(command.getValue().page()).isEqualTo("Dashboard");
        assertThat(command.getValue().tool()).isEqualTo("pos_get_sales_report");
    }

    @Test
    void summaryChat_acceptsExpandedSummarySize() throws Exception {
        when(answerDashboardSummaryUseCase.answer(any())).thenReturn("Here is the analysis.");
        String summary = "x".repeat(25000);

        mockMvc.perform(post("/api/v1/ai/dashboard/summary-chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Summarize today","page":"Report","tool":"pos_get_sales_report","summary":"%s"}
                                """.formatted(summary)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Here is the analysis."));
    }

    @Test
    void summaryChat_blankMessage_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/ai/dashboard/summary-chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"","page":"Dashboard","tool":"pos_get_sales_report","summary":"Sales: Rp 1.000.000"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
