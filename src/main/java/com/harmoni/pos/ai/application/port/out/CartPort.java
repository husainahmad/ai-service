package com.harmoni.pos.ai.application.port.out;

import com.harmoni.pos.ai.domain.model.Cart;
import com.harmoni.pos.ai.domain.model.CartLine;

import java.util.List;

public interface CartPort {

    /** Adds a line to the session cart and returns the resulting cart. */
    Cart addItem(String sessionId, CartLine line);

    /** Removes a product line entirely and returns the resulting cart. */
    Cart removeItem(String sessionId, Integer productId);

    Cart getCart(String sessionId);

    List<CartLine> lines(String sessionId);

    void clear(String sessionId);
}
