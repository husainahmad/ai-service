package com.harmoni.pos.ai.application.port.in;

import com.harmoni.pos.ai.domain.model.Recommendation;

public interface GenerateRecommendationUseCase {

    Recommendation generate(GenerateRecommendationCommand command);
}
