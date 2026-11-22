package com.harmoni.pos.ai.application.port.out;

import com.harmoni.pos.ai.domain.model.Recommendation;

public interface InsightPort {

    void save(Recommendation recommendation);
}
