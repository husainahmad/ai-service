package com.harmoni.pos.ai.adapter.out.cart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harmoni.pos.ai.application.port.out.CartPort;
import com.harmoni.pos.ai.config.MenuServiceProperties;
import com.harmoni.pos.ai.domain.model.Cart;
import com.harmoni.pos.ai.domain.model.CartLine;
import com.harmoni.pos.ai.domain.model.CustomizationGroupSelection;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cart per session. New lines get their authoritative name, SKU and
 * price from the Menu Service so the assistant never invents them.
 */
@Component
public class InMemoryCartAdapter implements CartPort {

    private final Map<String, List<CartLine>> carts = new ConcurrentHashMap<>();
    private final RestClient menuRestClient;
    private final MenuServiceProperties menuProps;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InMemoryCartAdapter(@Qualifier("menuRestClient") RestClient menuRestClient,
                               MenuServiceProperties menuProps) {
        this.menuRestClient = menuRestClient;
        this.menuProps = menuProps;
    }

    @Override
    public Cart addItem(String sessionId, CartLine line) {
        carts.computeIfAbsent(sessionId, key -> new ArrayList<>()).add(line);
        return getCart(sessionId);
    }

    /** Resolves product details from the Menu Service and adds a priced line. */
    public Cart addProduct(String sessionId, Integer productId, Long skuId, int quantity,
                           List<CustomizationGroupSelection> customizations) {
        CartLine line = resolveLine(productId, skuId, quantity, customizations);
        return addItem(sessionId, line);
    }

    private CartLine resolveLine(Integer productId, Long skuId, int quantity,
                                 List<CustomizationGroupSelection> customizations) {
        String name = null;
        BigDecimal price = BigDecimal.ZERO;
        Long resolvedSku = skuId;
        String endpoint = menuProps.getEndpoints().getProductById();
        try {
            String json = menuRestClient.get().uri(endpoint, productId).retrieve().body(String.class);
            if (json != null) {
                JsonNode root = objectMapper.readTree(json);
                JsonNode data = root.has("data") ? root.get("data") : root;
                if (data != null && data.isArray() && !data.isEmpty()) data = data.get(0);
                if (data != null) {
                    name = data.path("name").asText(null);
                    JsonNode skus = data.path("skus");
                    if (skus.isArray() && !skus.isEmpty()) {
                        if (resolvedSku == null) {
                            resolvedSku = skus.get(0).path("id").asLong(0);
                        }
                        for (JsonNode sku : skus) {
                            if (sku.path("id").asLong(0) == resolvedSku) {
                                price = extractPrice(sku);
                                break;
                            }
                        }
                    }
                    if (price.signum() == 0) {
                        price = extractPrice(data);
                    }
                }
            }
        } catch (Exception e) {
            // Keep a zero-priced line; the order tool will still carry the product id.
        }
        return CartLine.of(productId, resolvedSku, name, price, quantity, customizations);
    }

    /** Reads the authoritative price from skus[].tierPrices[].price (or a direct price field). */
    private BigDecimal extractPrice(JsonNode node) {
        JsonNode tierPrices = node.path("tierPrices");
        if (tierPrices.isArray()) {
            BigDecimal best = BigDecimal.ZERO;
            for (JsonNode tier : tierPrices) {
                BigDecimal candidate = decimalOf(tier.path("price").asText(null));
                if (candidate != null && candidate.compareTo(best) > 0) {
                    best = candidate;
                }
            }
            if (best.signum() > 0) return best;
        }
        BigDecimal direct = decimalOf(node.path("price").asText(null));
        return direct == null ? BigDecimal.ZERO : direct;
    }

    private static BigDecimal decimalOf(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return new BigDecimal(raw); } catch (Exception e) { return null; }
    }

    @Override
    public Cart removeItem(String sessionId, Integer productId) {
        List<CartLine> lines = carts.getOrDefault(sessionId, new ArrayList<>());
        lines.removeIf(line -> productId != null && productId.equals(line.productId()));
        return getCart(sessionId);
    }

    @Override
    public Cart getCart(String sessionId) {
        return new Cart(List.copyOf(carts.getOrDefault(sessionId, List.of())));
    }

    @Override
    public List<CartLine> lines(String sessionId) {
        return carts.getOrDefault(sessionId, List.of());
    }

    @Override
    public void clear(String sessionId) {
        carts.remove(sessionId);
    }
}
