package com.harmoni.pos.ai.adapter.in.web;

import com.harmoni.pos.ai.application.port.in.AddCartItemCommand;
import com.harmoni.pos.ai.application.port.in.ManageCartUseCase;
import com.harmoni.pos.ai.domain.model.CartView;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customer-sessions/{sessionId}/cart")
public class CartController {

    private final ManageCartUseCase manageCartUseCase;

    public CartController(ManageCartUseCase manageCartUseCase) {
        this.manageCartUseCase = manageCartUseCase;
    }

    @PostMapping("/items")
    public Map<String, Object> addItem(@PathVariable String sessionId,
                                       @RequestBody Map<String, Object> request) {
        BigDecimal price = request.get("price") == null
                ? BigDecimal.ZERO
                : new BigDecimal(request.get("price").toString());
        int quantity = request.get("quantity") == null ? 1 : Integer.parseInt(request.get("quantity").toString());
        return toMap(manageCartUseCase.addItem(new AddCartItemCommand(
                sessionId, integer(request.get("productId")), integer(request.get("skuId")), price, quantity)));
    }

    @GetMapping
    public Map<String, Object> view(@PathVariable String sessionId) {
        return toMap(manageCartUseCase.view(sessionId));
    }

    @DeleteMapping("/items/{productId}")
    public Map<String, Object> removeItem(@PathVariable String sessionId, @PathVariable Integer productId) {
        return toMap(manageCartUseCase.removeItem(sessionId, productId));
    }

    private static Map<String, Object> toMap(CartView view) {
        return Map.of("items", view.itemCount(), "total", view.total(), "lines", view.lines());
    }

    private static Integer integer(Object value) {
        return value == null ? null : Integer.valueOf(value.toString());
    }
}
