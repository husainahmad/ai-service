package com.harmoni.pos.ai.application.service;

import com.harmoni.pos.ai.application.port.in.AddCartItemCommand;
import com.harmoni.pos.ai.application.port.in.ManageCartUseCase;
import com.harmoni.pos.ai.application.port.out.CartPort;
import com.harmoni.pos.ai.domain.model.CartLine;
import com.harmoni.pos.ai.domain.model.CartView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartService implements ManageCartUseCase {

    private final CartPort cart;

    @Override
    public CartView addItem(AddCartItemCommand command) {
        cart.addItem(command.sessionId(), CartLine.of(
                command.productId(),
                command.skuId() == null ? null : command.skuId().longValue(),
                null,
                command.price(),
                command.quantity(),
                null));
        return toView(cart.getCart(command.sessionId()));
    }

    @Override
    public CartView view(String sessionId) {
        return toView(cart.getCart(sessionId));
    }

    @Override
    public CartView removeItem(String sessionId, Integer productId) {
        return toView(cart.removeItem(sessionId, productId));
    }

    private static CartView toView(com.harmoni.pos.ai.domain.model.Cart cart) {
        return new CartView(cart.totalItems(), cart.totalPrice(), cart.items());
    }
}
