package com.harmoni.pos.ai.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record CartView(int itemCount, BigDecimal total, List<CartLine> lines) {
}
