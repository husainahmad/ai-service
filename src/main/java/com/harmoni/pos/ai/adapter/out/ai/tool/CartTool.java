package com.harmoni.pos.ai.adapter.out.ai.tool;

import com.harmoni.pos.ai.adapter.out.ai.tool.dto.CustomizationGroupToolResponse;
import com.harmoni.pos.ai.adapter.out.ai.tool.dto.CustomizationOptionToolResponse;
import com.harmoni.pos.ai.adapter.out.cart.InMemoryCartAdapter;
import com.harmoni.pos.ai.domain.model.CustomizationGroupSelection;
import com.harmoni.pos.ai.domain.model.CustomizationOptionSelection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the shopping cart — adding items, checking what's inside, and removing items.
 * Everything is kept per session in {@link InMemoryCartAdapter}; identifiers and prices
 * come from the Menu Service tools only.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CartTool {

    private final InMemoryCartAdapter cartService;
    private final MenuTool menuTools;

    /** Plain overload for callers that already know the product has no groups. */
    public String addToOrder(Long sessionId, Long productId, Long skuId, Integer quantity) {
        if (sessionId == null) return ToolConstants.cartError("sessionId is required");
        if (productId == null) return ToolConstants.cartError("productId is required, call searchProductsByName first");
        int qty = (quantity == null || quantity <= 0) ? 1 : quantity;
        try {
            var cart = cartService.addProduct(String.valueOf(sessionId), productId.intValue(), skuId, qty, List.of());
            return ToolConstants.itemAdded(productId, qty, cart.totalItems(), cart.totalPrice().toPlainString());
        } catch (Exception e) {
            log.warn("addToOrder failed sessionId={} productId={} skuId={} qty={}: {}", sessionId, productId, skuId, qty, e.getMessage());
            return ToolConstants.cartError("unable to add productId=" + productId);
        }
    }

    @Tool(description = """
            Add a product to the customer's cart.

            You MUST call searchProductsByName first to get the real productId and skuId from the Menu Service.
            NEVER guess or invent productId or skuId.
            Always pass the skuId returned by searchProductsByName.

            If the product has multiple SKUs/variants, ask the customer which variant they want before adding.

            CUSTOMIZATIONS:
            Before adding, call getProductCustomizations(productId).
            - If it has no groups, omit customizationOptionIds.
            - If it has groups, ask the customer to choose first (respect required,
              min_selection, max_selection and selectionType). Then pass the chosen option
              ids in customizationOptionIds.
            Format: comma-separated option ids, optionally "optionId:quantity"
            (example: "55,58:2" means option 55 once and option 58 twice).
            NEVER invent option ids — use only the optionId values returned by the Menu Service.
            """)
    public String addToOrder(
            @ToolParam(description = "Customer session id, e.g. 42") Long sessionId,
            @ToolParam(description = "Product id from the Menu Service — call searchProductsByName first") Long productId,
            @ToolParam(description = "SKU id from the Menu Service — use the skuId returned by searchProductsByName") Long skuId,
            @ToolParam(description = "How many to add, defaults to 1") Integer quantity,
            @ToolParam(description = "Chosen customization option ids, comma-separated, each optionally optionId:quantity (e.g. '55,58:2'). Omit when the product has no groups.") String customizationOptionIds) {
        if (sessionId == null) return ToolConstants.cartError("sessionId is required");
        if (productId == null) return ToolConstants.cartError("productId is required, call searchProductsByName first");
        int qty = (quantity == null || quantity <= 0) ? 1 : quantity;

        List<CustomizationGroupSelection> selections;
        try {
            selections = resolveCustomizations(productId, customizationOptionIds);
        } catch (IllegalStateException e) {
            log.warn("addToOrder customization resolution failed sessionId={} productId={}: {}", sessionId, productId, e.getMessage());
            return ToolConstants.cartError(e.getMessage());
        }
        try {
            var cart = cartService.addProduct(String.valueOf(sessionId), productId.intValue(), skuId, qty, selections);
            return ToolConstants.itemAdded(productId, qty, cart.totalItems(), cart.totalPrice().toPlainString());
        } catch (Exception e) {
            log.warn("addToOrder failed sessionId={} productId={} skuId={} qty={}: {}", sessionId, productId, skuId, qty, e.getMessage());
            return ToolConstants.cartError("unable to add productId=" + productId);
        }
    }

    private List<CustomizationGroupSelection> resolveCustomizations(Long productId, String customizationOptionIds) {
        if (customizationOptionIds == null || customizationOptionIds.isBlank()) {
            return List.of();
        }
        List<CustomizationGroupToolResponse> groups = menuTools == null
                ? null : menuTools.getCustomizationGroups(productId);
        if (groups == null) {
            throw new IllegalStateException("unable to read customizations for productId=" + productId);
        }

        Map<Long, List<CustomizationOptionSelection>> byGroup = new LinkedHashMap<>();
        Map<Long, Long> optionToGroup = buildOptionToGroup(groups);
        for (String token : customizationOptionIds.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) continue;
            String[] parts = trimmed.split(":");
            Long optionId = parseLong(parts[0]);
            if (optionId == null) continue;
            Long customizationId = optionToGroup.get(optionId);
            if (customizationId == null) {
                log.warn("customization option {} does not belong to product {}", optionId, productId);
                continue;
            }
            int optionQty = parts.length > 1 ? parseInt(parts[1], 1) : 1;
            byGroup.computeIfAbsent(customizationId, key -> new ArrayList<>())
                    .add(new CustomizationOptionSelection(optionId, optionQty));
        }

        if (byGroup.isEmpty()) {
            throw new IllegalStateException("customizationOptionIds could not be matched — call getProductCustomizations first");
        }
        return byGroup.entrySet().stream()
                .map(entry -> new CustomizationGroupSelection(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static Map<Long, Long> buildOptionToGroup(List<CustomizationGroupToolResponse> groups) {
        Map<Long, Long> lookup = new java.util.HashMap<>();
        for (CustomizationGroupToolResponse group : groups) {
            if (group.options() == null || group.customizationId() == null) continue;
            for (CustomizationOptionToolResponse option : group.options()) {
                if (option.optionId() != null) {
                    lookup.put(option.optionId(), group.customizationId());
                }
            }
        }
        return lookup;
    }

    private static Long parseLong(String raw) {
        try { return Long.parseLong(raw.trim()); } catch (Exception e) { return null; }
    }

    private static int parseInt(String raw, int fallback) {
        try {
            int value = Integer.parseInt(raw.trim());
            return value < 1 ? fallback : value;
        } catch (Exception e) { return fallback; }
    }

    @Tool(description = "Check what's currently in the cart — items and total. Returns CART_EMPTY if there's nothing yet.")
    public String getCurrentOrder(@ToolParam(description = "Customer session id") Long sessionId) {
        if (sessionId == null) return ToolConstants.cartError("sessionId is required");
        try {
            var cart = cartService.getCart(String.valueOf(sessionId));
            if (cart.items().isEmpty()) return ToolConstants.cartEmpty();
            return cart.toString();
        } catch (Exception e) {
            log.warn("getCurrentOrder failed sessionId={}: {}", sessionId, e.getMessage());
            return ToolConstants.cartError("unable to read cart");
        }
    }

    @Tool(description = "Remove an item from the cart. You need the productId that's already in the cart.")
    public String removeOrderItem(
            @ToolParam(description = "Customer session id") Long sessionId,
            @ToolParam(description = "Product id to remove from the cart") Long productId) {
        if (sessionId == null) return ToolConstants.cartError("sessionId is required");
        if (productId == null) return ToolConstants.cartError("productId is required");
        try {
            var cart = cartService.removeItem(String.valueOf(sessionId), productId.intValue());
            return ToolConstants.itemRemoved(productId, cart.totalItems());
        } catch (Exception e) {
            log.warn("removeOrderItem failed sessionId={} productId={}: {}", sessionId, productId, e.getMessage());
            return ToolConstants.cartError("unable to remove productId=" + productId);
        }
    }
}
