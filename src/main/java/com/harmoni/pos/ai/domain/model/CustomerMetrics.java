package com.harmoni.pos.ai.domain.model;

import java.math.BigDecimal;

public record CustomerMetrics(Long customerId, BigDecimal totalSpend, int visitCount,
                              BigDecimal avgOrderValue, String segment) {
}
