package com.harmoni.pos.ai.application.port.in;

import java.math.BigDecimal;

public record AddCartItemCommand(String sessionId, Integer productId, Integer skuId,
                                 BigDecimal price, int quantity) {
}
