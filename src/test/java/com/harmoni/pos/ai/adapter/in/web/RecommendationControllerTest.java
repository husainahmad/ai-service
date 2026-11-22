package com.harmoni.pos.ai.adapter.in.web;

import com.harmoni.pos.ai.application.port.in.GenerateRecommendationUseCase;
import com.harmoni.pos.ai.domain.model.Recommendation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecommendationController.class)
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GenerateRecommendationUseCase generateRecommendationUseCase;

    @Test
    void recommend_returnsGeneratedText() throws Exception {
        when(generateRecommendationUseCase.generate(any())).thenReturn(new Recommendation(
                7L, "AI recommendation", "Offer a loyalty discount on their next visit.", new BigDecimal("0.85")));

        mockMvc.perform(get("/api/v1/ai/recommendations/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(7))
                .andExpect(jsonPath("$.recommendation").value("Offer a loyalty discount on their next visit."));
    }
}
