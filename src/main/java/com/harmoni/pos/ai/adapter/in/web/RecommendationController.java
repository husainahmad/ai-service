package com.harmoni.pos.ai.adapter.in.web;

import com.harmoni.pos.ai.adapter.in.web.dto.RecommendationResponse;
import com.harmoni.pos.ai.application.port.in.GenerateRecommendationCommand;
import com.harmoni.pos.ai.application.port.in.GenerateRecommendationUseCase;
import com.harmoni.pos.ai.domain.model.Recommendation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recommendations", description = "AI-generated customer recommendations")
public class RecommendationController {

    private final GenerateRecommendationUseCase generateRecommendationUseCase;

    @Operation(summary = "Generate and persist a recommendation for a customer")
    @GetMapping("/{customerId}")
    public RecommendationResponse recommend(@PathVariable long customerId) {
        Recommendation recommendation = generateRecommendationUseCase.generate(
                new GenerateRecommendationCommand(customerId));
        return new RecommendationResponse(recommendation.customerId(), recommendation.body());
    }
}
