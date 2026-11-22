package com.harmoni.pos.ai.adapter.out.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.harmoni.pos.ai.adapter.out.ai.tool.dto.CategorySearchToolResponse;
import com.harmoni.pos.ai.adapter.out.ai.tool.dto.CategoryToolResponse;
import com.harmoni.pos.ai.adapter.out.ai.tool.dto.CustomizationGroupToolResponse;
import com.harmoni.pos.ai.adapter.out.ai.tool.dto.ProductCustomizationsToolResponse;
import com.harmoni.pos.ai.adapter.out.ai.tool.dto.ProductSearchToolResponse;
import com.harmoni.pos.ai.adapter.out.ai.tool.dto.ProductToolResponse;
import com.harmoni.pos.ai.config.MenuServiceProperties;
import com.harmoni.pos.ai.domain.exception.MenuServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Answers anything about the menu — product search, categories, and prices.
 * <p>
 * This is the menu side of the AI toolbox. It talks to the Menu Service (8082)
 * so the assistant never has to guess product names, IDs, or prices.
 * The LLM decides <em>when</em> to call, this class decides <em>how</em> to fetch and format.
 * All endpoint paths come from {@link MenuServiceProperties} (i.e. application.yaml).
 * <p>
 * <strong>Tool responses are structured JSON containing authoritative identifiers.</strong>
 * The LLM must use these identifiers (productId, skuId, categoryId) for all subsequent operations.
 * Never invent or guess database identifiers.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MenuTool {

    @Qualifier("menuRestClient")
    private final RestClient menuRestClient;

    private final MenuServiceProperties menuProps;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Bounds customization lookups when formatting a large product list. */
    private static final int MAX_CUSTOMIZATION_LOOKUPS = 20;

    // --- product search ---

    @Tool(description = """
            ALWAYS call this tool when the customer mentions a specific product or asks about a product's price/availability.

            NEVER guess or invent productId, skuId, categoryId, or price.
            NEVER claim a product is unavailable before calling this tool.
            ALWAYS use the identifiers returned by the Menu Service.

            The response contains structured JSON with:
            - productId: authoritative database ID for the product
            - skuId: authoritative database ID for the SKU (use for addToOrder)
            - name: product name
            - description: product description (if available)
            - price: price in IDR from the Menu Service
            - skus: list of available SKUs/variants with their own skuId and price
            - has_customizations: true when the product has options the customer must choose
            - customizations: the groups and options, when the Menu Service returned them

            If has_customizations is true but customizations is missing, call
            getProductCustomizations before adding the product to the cart.

            Good calls:
            - "harga caramel macchiato" -> searchProductsByName("caramel macchiato")
            - "ada kopi tubruk?" -> searchProductsByName("kopi tubruk")
            - "cari americano" -> searchProductsByName("americano")

            Do NOT use this to show the full menu — use getCategoriesByBrand instead.
            """)
    public String searchProductsByName(
            @ToolParam(description = "Product name as the customer typed it, e.g. 'caramel macchiato'") String productName) {
        if (productName == null || productName.isBlank()) {
            return toJson(ProductSearchToolResponse.notFound("productName is required"));
        }
        String query = normalizeProductQuery(productName);
        String endpoint = menuProps.getEndpoints().getProductSearch();
        try {
            return fetchAndFormatProducts(endpoint, query, query);
        } catch (Exception e) {
            log.warn("searchProductsByName failed name='{}': {}", query, e.getMessage());
            return toJson(ProductSearchToolResponse.error("unable to search products"));
        }
    }

    private String fetchAndFormatProducts(String endpoint, String query, String context) {
        String json = menuRestClient.get()
                .uri(uri -> uri.path(endpoint).queryParam("productName", query).build())
                .retrieve().body(String.class);
        return formatProductsJson(json, context);
    }

    private String normalizeProductQuery(String raw) {
        if (raw == null) return "";
        return raw.trim().replaceAll("[\\?\\!\\.]+$", "").trim();
    }

    // --- product customizations ---

    @Tool(description = """
            Check whether a product has customization options, and list them.

            ALWAYS call this tool before adding a product to the cart when the customer
            wants to order it. Calling it first is how the assistant knows whether it
            must ask the customer to choose anything.

            The response contains structured JSON with:
            - productId: the product that was checked
            - has_customizations: true when the product has groups to choose from
            - customizations: one entry per group, with:
                - customizationId: authoritative group id
                - name: group name, e.g. "Size" or "Milk"
                - selectionType: SINGLE (pick one) or MULTIPLE (pick several)
                - required / min_selection / max_selection: how many options are needed
                - allow_quantity: true when an option may be chosen more than once
                - options: each with optionId, name and price (extra charge in IDR)

            When has_customizations is true, DO NOT add the product yet: ask the customer
            to choose, using only the optionId values returned here. Then call addToOrder
            with customization_option_ids.
            When it is false, the product can be added directly.
            """)
    public String getProductCustomizations(
            @ToolParam(description = "Product id from the Menu Service — call searchProductsByName first") Long productId) {
        if (productId == null) {
            return toJson(ProductCustomizationsToolResponse.error(null, "productId is required"));
        }
        List<CustomizationGroupToolResponse> groups = getCustomizationGroups(productId);
        if (groups == null) {
            return toJson(ProductCustomizationsToolResponse.error(productId,
                    "unable to read customizations for productId=" + productId));
        }
        return toJson(ProductCustomizationsToolResponse.success(productId, groups));
    }

    public List<CustomizationGroupToolResponse> getCustomizationGroups(Long productId) {
        if (productId == null) {
            return List.of();
        }
        String endpoint = menuProps.getEndpoints().getProductCustomization();
        try {
            String json = menuRestClient.get()
                    .uri(endpoint, productId)
                    .header(ToolConstants.HEADER_X_USERNAME, ToolConstants.DEFAULT_USERNAME)
                    .retrieve().body(String.class);
            return parseCustomizationGroupsJson(json);
        } catch (Exception e) {
            log.warn("getCustomizationGroups failed productId={}: {}", productId, e.getMessage());
            return null;
        }
    }

    private List<CustomizationGroupToolResponse> parseCustomizationGroupsJson(String json) {
        try {
            return JsonNodeExtractor.extractDataArray(objectMapper.readTree(json)).stream()
                    .map(node -> JsonNodeExtractor.from(node, objectMapper).asCustomizationGroup())
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.warn("parseCustomizationGroupsJson failed: {}", e.getMessage());
            return List.of();
        }
    }

    // --- categories ---

    @Tool(description = """
            ALWAYS call this tool when the customer asks to search for a category by name.

            NEVER guess or invent categoryId.
            ALWAYS use the categoryId returned by the Menu Service.

            The response contains structured JSON with:
            - categoryId: authoritative database ID for the category
            - name: category name
            - description: category description (if available)
            """)
    public String searchCategoriesByName(
            @ToolParam(description = "Category name as the customer typed it, e.g. 'Coffee'") String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return toJson(CategorySearchToolResponse.notFound("categoryName is required"));
        }
        String query = categoryName.trim();
        String endpoint = menuProps.getEndpoints().getCategorySearch();
        try {
            String json = menuRestClient.get()
                    .uri(uri -> uri.path(endpoint).queryParam("categoryName", query).build())
                    .retrieve().body(String.class);
            return formatCategoriesJson(json);
        } catch (Exception e) {
            log.warn("searchCategoriesByName failed name='{}': {}", query, e.getMessage());
            return toJson(CategorySearchToolResponse.error("unable to search categories"));
        }
    }

    /** Raw JSON for REST callers — not an LLM tool. */
    public String getCategoriesByBrandRaw(Integer brandId) {
        int bid = brandId == null ? ToolConstants.DEFAULT_BRAND_ID : brandId;
        try {
            return fetchCategoriesRaw(bid);
        } catch (Exception e) {
            log.warn("getCategoriesByBrandRaw failed brandId={}: {}", bid, e.getMessage());
            throw new MenuServiceUnavailableException("unable to fetch categories for brand " + bid, e);
        }
    }

    private String fetchCategoriesRaw(int brandId) {
        String endpoint = menuProps.getEndpoints().getCategoryByBrand();
        return menuRestClient.get().uri(endpoint, brandId).retrieve().body(String.class);
    }

    @Tool(description = """
            ALWAYS call this tool when the customer wants to see the menu, categories, or asks "what's on the menu?".

            NEVER guess or invent categoryId.
            ALWAYS use the categoryId returned by the Menu Service.

            The response contains structured JSON with:
            - categoryId: authoritative database ID for the category
            - name: category name
            - description: category description (if available)

            This is the primary tool for showing the menu. Do not use searchProductsByName for this.
            """)
    public String getCategoriesByBrand() {
        return getCategoriesByBrand(ToolConstants.DEFAULT_BRAND_ID);
    }

    public String getCategoriesByBrand(Integer brandId) {
        int bid = brandId == null ? ToolConstants.DEFAULT_BRAND_ID : brandId;
        try {
            return formatCategoriesJson(fetchCategoriesRaw(bid));
        } catch (Exception e) {
            log.warn("getCategoriesByBrand failed brandId={}: {}", bid, e.getMessage());
            return toJson(CategorySearchToolResponse.error("unable to list categories"));
        }
    }

    @Tool(description = """
            ALWAYS call this tool when the customer wants to see products in a specific category.

            You MUST have a valid categoryId from getCategoriesByBrand or searchCategoriesByName first.
            NEVER guess or invent categoryId.

            The response contains structured JSON with:
            - productId: authoritative database ID for each product
            - skuId: authoritative database ID for the SKU (use for addToOrder)
            - name: product name
            - description: product description (if available)
            - price: price in IDR from the Menu Service
            - skus: list of available SKUs/variants with their own skuId and price
            - has_customizations: true when the product has options the customer must choose

            Before adding any product to the cart, call getProductCustomizations for it.
            """)
    public String getProductsByCategory(
            @ToolParam(description = "Category id from the Menu Service, e.g. 13 for Coffee") Integer categoryId) {
        if (categoryId == null) {
            return toJson(ProductSearchToolResponse.notFound("categoryId is required, call searchCategoriesByName or getCategoriesByBrand first"));
        }
        String priceEndpoint = menuProps.getEndpoints().getProductByCategoryPrice();
        String pagedEndpoint = menuProps.getEndpoints().getProductByCategoryPaged();
        try {
            String json = menuRestClient.get()
                    .uri(priceEndpoint, categoryId)
                    .header(ToolConstants.HEADER_X_USERNAME, ToolConstants.DEFAULT_USERNAME)
                    .retrieve().body(String.class);
            if (json == null) return toJson(ProductSearchToolResponse.notFound("no products for categoryId=" + categoryId));
            return formatProductsJson(json, "categoryId=" + categoryId);
        } catch (Exception e) {
            log.warn("getProductsByCategory price endpoint failed categoryId={}: {}", categoryId, e.getMessage());
            try {
                String first = menuRestClient.get()
                        .uri(uri -> uri.path(pagedEndpoint)
                                .queryParam("page", 1).queryParam("size", 100).queryParam("search", "")
                                .build(categoryId, ToolConstants.DEFAULT_BRAND_ID))
                        .retrieve().body(String.class);
                if (first == null) return toJson(ProductSearchToolResponse.notFound("no products for categoryId=" + categoryId));
                return formatProductsJson(first, "categoryId=" + categoryId);
            } catch (Exception e2) {
                return toJson(ProductSearchToolResponse.error("unable to list products for categoryId=" + categoryId));
            }
        }
    }

    // --- JSON formatters for LLM tool responses ---

    private String formatCategoriesJson(String json) {
        try {
            List<JsonNodeExtractor> extractors = JsonNodeExtractor.extractAllCategories(json, objectMapper);
            if (extractors.isEmpty()) return toJson(CategorySearchToolResponse.notFound("no categories found"));

            List<CategoryToolResponse> categories = extractors.stream()
                    .map(ext -> CategoryToolResponse.simple(ext.productId(), ext.name()))
                    .collect(Collectors.toList());

            return toJson(CategorySearchToolResponse.success(categories));
        } catch (Exception e) {
            log.warn("formatCategoriesJson failed: {}", e.getMessage());
            return toJson(CategorySearchToolResponse.error("invalid category data"));
        }
    }

    private String formatProductsJson(String json, String context) {
        try {
            List<JsonNodeExtractor> extractors = JsonNodeExtractor.extractAllProducts(json, objectMapper);
            if (extractors.isEmpty()) return toJson(ProductSearchToolResponse.notFound(context));

            List<ProductToolResponse> products = new ArrayList<>(extractors.size());
            int lookups = 0;
            for (JsonNodeExtractor extractor : extractors) {
                ProductToolResponse product = extractor.toProductToolResponse();
                if (product.customizations() == null
                        && product.productId() != null
                        && lookups < MAX_CUSTOMIZATION_LOOKUPS) {
                    lookups++;
                    List<CustomizationGroupToolResponse> groups =
                            getCustomizationGroups(product.productId());
                    if (groups != null) {
                        product = product.withCustomizations(groups);
                    }
                }
                products.add(product);
            }

            return toJson(ProductSearchToolResponse.success(products));
        } catch (Exception e) {
            log.warn("formatProductsJson failed context={}: {}", context, e.getMessage());
            return toJson(ProductSearchToolResponse.error("invalid product data"));
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Failed to serialize tool response: {}", e.getMessage());
            return "{\"error\":\"MENU_SERVICE_ERROR: serialization failed\"}";
        }
    }
}
