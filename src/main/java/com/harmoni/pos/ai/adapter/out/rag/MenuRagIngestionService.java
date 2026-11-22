package com.harmoni.pos.ai.adapter.out.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.harmoni.pos.ai.adapter.out.ai.tool.JsonNodeExtractor;
import com.harmoni.pos.ai.config.AiProperties;
import com.harmoni.pos.ai.config.MenuServiceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ingests menu categories and products into the in-memory vector store on
 * startup when {@code harmoni.ai.rag.enabled=true}. RAG is a convenience layer;
 * the tools remain the source of truth for identifiers and prices.
 */
@Slf4j
@Component
public class MenuRagIngestionService {

    private static final String TYPE = "type";
    private static final String ID = "id";
    private static final String NAME = "name";

    private final VectorStore vectorStore;
    private final RestClient menuRestClient;
    private final MenuServiceProperties menuProps;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MenuRagIngestionService(VectorStore vectorStore,
                                   @Qualifier("menuRestClient") RestClient menuRestClient,
                                   MenuServiceProperties menuProps,
                                   AiProperties aiProperties) {
        this.vectorStore = vectorStore;
        this.menuRestClient = menuRestClient;
        this.menuProps = menuProps;
        this.aiProperties = aiProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ingestOnStartup() {
        if (!aiProperties.isRagEnabled()) {
            log.debug("RAG disabled — skipping menu ingestion");
            return;
        }
        try {
            int count = ingest();
            log.info("RAG menu ingestion complete: {} documents", count);
        } catch (Exception e) {
            log.warn("RAG menu ingestion failed: {}", e.getMessage());
        }
    }

    /** Fetches categories and their products from the Menu Service and stores them as documents. */
    public int ingest() {
        List<Document> documents = new ArrayList<>();

        String categoryJson = menuRestClient.get()
                .uri(uri -> uri.path(menuProps.getEndpoints().getCategoryByBrand())
                        .build(menuProps.getBrandId()))
                .retrieve().body(String.class);
        List<JsonNodeExtractor> categories = JsonNodeExtractor.extractAllCategories(categoryJson, objectMapper);
        for (JsonNodeExtractor category : categories) {
            Map<String, Object> meta = new HashMap<>();
            meta.put(TYPE, "category");
            meta.put(ID, category.productId() == null ? 0 : category.productId());
            meta.put(NAME, category.name() == null ? "" : category.name());
            documents.add(new Document(
                    "Menu category: " + category.name()
                            + (category.description() == null ? "" : " — " + category.description()),
                    meta));
        }

        String priceEndpoint = menuProps.getEndpoints().getProductByCategoryPrice();
        for (JsonNodeExtractor category : categories) {
            if (category.productId() == null) continue;
            try {
                String productsJson = menuRestClient.get()
                        .uri(priceEndpoint, category.productId())
                        .retrieve().body(String.class);
                for (JsonNodeExtractor product : JsonNodeExtractor.extractAllProducts(productsJson, objectMapper)) {
                    Map<String, Object> meta = new HashMap<>();
                    meta.put(TYPE, "product");
                    meta.put(ID, product.productId());
            meta.put(NAME, product.name() == null ? "" : product.name());
            meta.put("categoryId", category.productId());
            meta.put("price", product.price() == null ? 0 : product.price());
                    documents.add(new Document(
                            "Menu product: " + product.name()
                                    + " in category " + category.name()
                                    + " priced " + product.price()
                                    + (product.description() == null ? "" : " — " + product.description()),
                            meta));
                }
            } catch (Exception e) {
                log.warn("RAG ingestion for category {} failed: {}", category.productId(), e.getMessage());
            }
        }

        if (!documents.isEmpty()) {
            vectorStore.add(documents);
        }
        return documents.size();
    }
}
