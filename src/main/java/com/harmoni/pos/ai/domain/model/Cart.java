package com.harmoni.pos.ai.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record Cart(List<CartLine> items) {

    public static Cart empty() {
        return new Cart(List.of());
    }

    public int totalItems() {
        return items.stream().mapToInt(CartLine::quantity).sum();
    }

    public BigDecimal totalPrice() {
        return items.stream()
                .map(CartLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public String toString() {
        if (items.isEmpty()) {
            return "CART_EMPTY";
        }
        StringBuilder sb = new StringBuilder("Cart (").append(totalItems()).append(" items, total=")
                .append(totalPrice().toPlainString()).append("):");
        for (CartLine line : items) {
            sb.append("\n- ").append(line.name() == null ? "product" : line.name())
                    .append(" x").append(line.quantity())
                    .append(" = ").append(line.lineTotal().toPlainString());
        }
        return sb.toString();
    }
}
