package com.harmoni.pos.ai.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * One cart line. Prices come from the Menu Service; ids are authoritative.
 */
public record CartLine(Integer productId, Long skuId, String name, int quantity,
                       BigDecimal price, BigDecimal lineTotal,
                       List<CustomizationGroupSelection> customizations) {

    public static CartLine of(Integer productId, Long skuId, String name, BigDecimal price, int quantity,
                              List<CustomizationGroupSelection> customizations) {
        BigDecimal unitPrice = price == null ? BigDecimal.ZERO : price;
        return new CartLine(productId, skuId, name, quantity, unitPrice,
                unitPrice.multiply(BigDecimal.valueOf(quantity)),
                customizations == null ? List.of() : customizations);
    }

    public CartLine withCustomizations(List<CustomizationGroupSelection> groups) {
        return new CartLine(productId, skuId, name, quantity, price, lineTotal, groups);
    }
}
