package com.harmoni.pos.ai.adapter.out.ai.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harmoni.pos.ai.application.port.out.AiSessionPort;
import com.harmoni.pos.ai.application.port.out.CartPort;
import com.harmoni.pos.ai.application.port.out.CustomerDataPort;
import com.harmoni.pos.ai.config.MenuServiceProperties;
import com.harmoni.pos.ai.config.OrderServiceProperties;
import com.harmoni.pos.ai.domain.model.AiSession;
import com.harmoni.pos.ai.domain.model.CartLine;
import com.harmoni.pos.ai.domain.model.CustomizationGroupSelection;
import com.harmoni.pos.ai.domain.model.CustomerProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a cart into a real order — only when the customer explicitly confirms.
 * Checkout is two calls on the same path: POST /order creates, PUT /order records payment.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderTool {

    @Qualifier("orderRestClient")
    private final RestClient orderRestClient;

    @Qualifier("menuRestClient")
    private final RestClient menuRestClient;

    private final CartPort cartPort;
    private final AiSessionPort sessionPort;
    private final CustomerDataPort customerDataPort;
    private final MenuServiceProperties menuProps;
    private final OrderServiceProperties orderProps;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Tool(description = """
            Place the order from the current cart AND record its payment method. Only call this after the customer clearly confirms — like 'ya', 'pesan sekarang', or 'checkout'.
            FIRST ask the customer which payment method they want: Tunai/Cash, QRIS, or Kartu/Debit.
            paymentMethod is required. Valid values: 'tunai' (Cash, paymentId 1), 'qris' (QRIS, paymentId 2), 'kartu' (Kartu/Debit, paymentId 3).
            note is optional — pass any special request the customer mentioned (e.g. 'es extra', 'tanpa gula', 'pesan dibungkus').
            Creates the order (POST) then marks it paid (PUT). Will reject an empty cart.
            """)
    public String confirmOrder(
            @ToolParam(description = "Customer session id") Long sessionId,
            @ToolParam(description = "Customer name for the order, e.g. 'Budi' — if not provided, the registered customer name for the session is used") String username,
            @ToolParam(description = "Payment method chosen by the customer: tunai/cash, qris/qr, or kartu/card/debit") String paymentMethod,
            @ToolParam(description = "Optional special request/note for the order, e.g. 'es extra', 'tanpa gula' — omit when none") String note) {
        if (sessionId == null) return ToolConstants.orderError("sessionId is required");
        Integer paymentId = resolvePaymentId(paymentMethod);
        if (paymentId == null) {
            return ToolConstants.orderError("paymentMethod required — ask the customer to choose: Tunai/Cash, QRIS, or Kartu/Debit");
        }
        try {
            AiSession session = sessionPort.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
            var cart = cartPort.getCart(String.valueOf(sessionId));
            if (cart.items().isEmpty()) return ToolConstants.cartEmpty("cannot confirm empty cart");

            Map<String, Object> orderDto = buildOrderDto(session, cart.items(),
                    resolveCustomerName(session.getCustomerId(), username), note);
            String endpoint = orderProps.getEndpoints().getCreateOrder();
            Integer orderId = extractOrderId(post(endpoint, orderDto));
            if (orderId == null) {
                return ToolConstants.orderError("order was created but the response had no orderId");
            }

            String payResp = put(endpoint, Map.of("orderId", orderId, "paymentId", paymentId));
            cartPort.clear(String.valueOf(sessionId));
            return ToolConstants.orderCreated(describePaidOrder(orderId, payResp, paymentId));
        } catch (Exception e) {
            log.warn("confirmOrder failed sessionId={}: {}", sessionId, e.getMessage(), e);
            return ToolConstants.orderError("unable to create or pay the order");
        }
    }

    private Map<String, Object> buildOrderDto(AiSession session, List<CartLine> items,
                                              String customerName, String note) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("storeId", orderProps.getStoreId());
        dto.put("customerId", session.getCustomerId());
        dto.put("customerName", customerName);
        dto.put("storeServiceTypesId", orderProps.getStoreServiceTypesId());
        dto.put("orderDetails", items.stream().map(this::toOrderDetail).toList());
        if (note != null && !note.isBlank()) dto.put("remark", note.trim());
        return dto;
    }

    private Map<String, Object> toOrderDetail(CartLine line) {
        Long skuId = line.skuId() != null ? line.skuId() : resolveSkuId(line.productId());
        if (skuId == null) skuId = line.productId() == null ? null : line.productId().longValue();

        Map<String, Object> sku = new LinkedHashMap<>();
        sku.put("skuId", skuId);
        sku.put("skuName", line.name());
        sku.put("quantity", line.quantity());
        List<Map<String, Object>> customizations = toCustomizationMaps(line.customizations());
        if (!customizations.isEmpty()) sku.put("customizations", customizations);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("productId", line.productId());
        detail.put("orderDetailSkus", List.of(sku));
        return detail;
    }

    private List<Map<String, Object>> toCustomizationMaps(List<CustomizationGroupSelection> groups) {
        if (groups == null) return List.of();
        return groups.stream()
                .filter(g -> g != null && g.customizationId() != null
                        && g.options() != null && !g.options().isEmpty())
                .map(g -> {
                    List<Map<String, Object>> options = g.options().stream()
                            .filter(o -> o != null && o.customizationOptionId() != null)
                            .map(o -> Map.<String, Object>of(
                                    "customizationOptionId", o.customizationOptionId(),
                                    "quantity", o.quantity() == null ? 1 : o.quantity()))
                            .toList();
                    return options.isEmpty() ? null
                            : Map.<String, Object>of("customizationId", g.customizationId(), "options", options);
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private String post(String endpoint, Object body) {
        return orderRestClient.post().uri(endpoint)
                .header(ToolConstants.HEADER_X_USERNAME, orderProps.getDefaultUsername())
                .body(body).retrieve().body(String.class);
    }

    private String put(String endpoint, Object body) {
        return orderRestClient.put().uri(endpoint)
                .header(ToolConstants.HEADER_X_USERNAME, orderProps.getDefaultUsername())
                .body(body).retrieve().body(String.class);
    }

    private String resolveCustomerName(Long customerId, String username) {
        if (username != null && !username.isBlank()) return username;
        if (customerId != null) {
            try {
                return customerDataPort.findCustomer(customerId)
                        .map(CustomerProfile::name)
                        .filter(n -> n != null && !n.isBlank())
                        .orElse(ToolConstants.DEFAULT_CUSTOMER_NAME);
            } catch (Exception e) {
                log.warn("resolveCustomerName failed customerId={}: {}", customerId, e.getMessage());
            }
        }
        return ToolConstants.DEFAULT_CUSTOMER_NAME;
    }

    private static Integer resolvePaymentId(String method) {
        if (method == null) return null;
        String m = method.trim().toLowerCase();
        if (m.matches("tunai|cash|uang|1")) return 1;
        if (m.matches("qris|qr|scan|2")) return 2;
        if (m.matches("kartu|card|debit|credit|kredit|3")) return 3;
        return null;
    }

    private String paymentName(Integer paymentId) {
        return switch (paymentId) {
            case 1 -> "Tunai";
            case 2 -> "QRIS";
            case 3 -> "Kartu";
            default -> "paymentId=" + paymentId;
        };
    }

    private Integer extractOrderId(String json) {
        if (json == null) return null;
        try {
            JsonNode data = unwrapData(objectMapper.readTree(json));
            long id = data != null ? data.path("id").asLong(0) : 0;
            return id == 0 ? null : (int) id;
        } catch (Exception e) {
            log.warn("extractOrderId failed: {}", e.getMessage());
            return null;
        }
    }

    /** Unwraps the Menu/Order Service RestAPIResponse data (and nested data.data) envelope. */
    private JsonNode unwrapData(JsonNode root) {
        if (root == null) return null;
        JsonNode data = root.has("data") ? root.get("data") : root;
        if (data != null && data.isObject() && data.has("data") && data.get("data").isObject()) {
            data = data.get("data");
        }
        return data;
    }

    private String describePaidOrder(Integer orderId, String payResp, Integer paymentId) {
        StringBuilder sb = new StringBuilder("orderId=" + orderId).append(" payment=").append(paymentName(paymentId));
        if (payResp != null) {
            try {
            JsonNode data = unwrapData(objectMapper.readTree(payResp));
            String orderNo = data != null ? data.path("orderNo").asText(null) : null;
                String grandTotal = data != null ? data.path("grandTotal").asText(null) : null;
                if (orderNo != null && !orderNo.isBlank()) sb.append(" orderNo=").append(orderNo);
                if (grandTotal != null && !grandTotal.isBlank() && !grandTotal.equals("0")) sb.append(" total=").append(grandTotal);
            } catch (Exception e) {
                log.warn("describePaidOrder failed: {}", e.getMessage());
            }
        }
        return sb.toString();
    }

    private Long resolveSkuId(Integer productId) {
        if (productId == null) return null;
        String endpoint = menuProps.getEndpoints().getProductById();
        try {
            String json = menuRestClient.get()
                    .uri(endpoint, productId)
                    .header(ToolConstants.HEADER_X_USERNAME, orderProps.getDefaultUsername())
                    .retrieve().body(String.class);
            if (json == null) return null;
            JsonNode root = objectMapper.readTree(json);
            JsonNode data = root.has("data") ? root.get("data") : root;
            if (data != null && data.isArray() && !data.isEmpty()) data = data.get(0);
            if (data != null && data.has("skus") && data.get("skus").isArray() && !data.get("skus").isEmpty()) {
                long sid = data.get("skus").get(0).path("id").asLong(0);
                if (sid != 0) return sid;
            }
        } catch (Exception e) {
            log.warn("resolveSkuId failed productId={}: {}", productId, e.getMessage());
        }
        return null;
    }
}
