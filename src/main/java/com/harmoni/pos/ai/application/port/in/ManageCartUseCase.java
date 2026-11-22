package com.harmoni.pos.ai.application.port.in;

import com.harmoni.pos.ai.domain.model.CartView;

public interface ManageCartUseCase {

    CartView addItem(AddCartItemCommand command);

    CartView view(String sessionId);

    CartView removeItem(String sessionId, Integer productId);
}
