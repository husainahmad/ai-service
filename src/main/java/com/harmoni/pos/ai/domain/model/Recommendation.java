package com.harmoni.pos.ai.domain.model;

import java.math.BigDecimal;

public record Recommendation(Long customerId, String title, String body, BigDecimal confidence) {
}
