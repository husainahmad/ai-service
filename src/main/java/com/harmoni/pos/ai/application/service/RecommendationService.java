package com.harmoni.pos.ai.application.service;

import com.harmoni.pos.ai.application.port.in.GenerateRecommendationCommand;
import com.harmoni.pos.ai.application.port.in.GenerateRecommendationUseCase;
import com.harmoni.pos.ai.application.port.out.CustomerDataPort;
import com.harmoni.pos.ai.application.port.out.InsightPort;
import com.harmoni.pos.ai.application.port.out.LlmPort;
import com.harmoni.pos.ai.domain.model.CustomerMetrics;
import com.harmoni.pos.ai.domain.model.Recommendation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class RecommendationService implements GenerateRecommendationUseCase {

    private static final BigDecimal CONFIDENCE = new BigDecimal("0.85");

    private final CustomerDataPort customerData;
    private final LlmPort llm;
    private final InsightPort insights;

    @Override
    public Recommendation generate(GenerateRecommendationCommand command) {
        CustomerMetrics metrics = customerData.getMetrics(command.customerId());
        String prompt = "Give one concise, personalized recommendation for this restaurant customer. "
                + "Do not invent data. Customer metrics: " + metrics;
        String text = llm.generate(command.customerId(), prompt);
        if (text == null || text.isBlank()) {
            text = "No recommendation available at the moment.";
        }
        Recommendation recommendation = new Recommendation(
                command.customerId(), "AI recommendation", text, CONFIDENCE);
        insights.save(recommendation);
        return recommendation;
    }
}
