package com.harmoni.pos.ai.adapter.out.client;

import com.harmoni.pos.ai.application.port.out.AiSessionPort;
import com.harmoni.pos.ai.application.port.out.CustomerDataPort;
import com.harmoni.pos.ai.application.port.out.InsightPort;
import com.harmoni.pos.ai.config.CustomerServiceProperties;
import com.harmoni.pos.ai.domain.model.AiSession;
import com.harmoni.pos.ai.domain.model.ChatMessage;
import com.harmoni.pos.ai.domain.model.CustomerMetrics;
import com.harmoni.pos.ai.domain.model.CustomerProfile;
import com.harmoni.pos.ai.domain.model.Recommendation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class CustomerServiceAdapter implements AiSessionPort, CustomerDataPort, InsightPort {

    private final RestClient client;
    private final CustomerServiceProperties properties;

    public CustomerServiceAdapter(@Qualifier("customerRestClient") RestClient client,
                                  CustomerServiceProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public Optional<AiSession> findById(long sessionId) {
        SessionResponse response = client.get()
                .uri(properties.getSession(), Map.of("sessionId", sessionId))
                .retrieve()
                .body(SessionResponse.class);
        return Optional.ofNullable(response)
                .map(value -> new AiSession(value.id(), value.customerId(), value.status()));
    }

    @Override
    public void saveMessage(long sessionId, ChatMessage message) {
        client.post()
                .uri(properties.getMessages(), Map.of("sessionId", sessionId))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("role", message.role(), "message", message.content()))
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public Optional<CustomerProfile> findCustomer(long customerId) {
        CustomerResponse response = client.get()
                .uri(properties.getCustomer(), Map.of("customerId", customerId))
                .retrieve()
                .body(CustomerResponse.class);
        return Optional.ofNullable(response)
                .map(value -> new CustomerProfile(value.id(), value.name(), value.phone()));
    }

    @Override
    public CustomerMetrics getMetrics(long customerId) {
        MetricsResponse response = client.get()
                .uri(properties.getMetrics(), Map.of("customerId", customerId))
                .retrieve()
                .body(MetricsResponse.class);
        if (response == null) {
            throw new IllegalStateException("Customer metrics unavailable for " + customerId);
        }
        return new CustomerMetrics(response.customerId(), response.totalSpend(),
                response.visitCount(), response.avgOrderValue(), response.segment());
    }

    @Override
    public void save(Recommendation recommendation) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (recommendation.customerId() != null) {
            payload.put("customerId", recommendation.customerId());
        }
        payload.put("type", "RECOMMENDATION");
        payload.put("title", recommendation.title());
        payload.put("body", recommendation.body());
        payload.put("confidence", recommendation.confidence());
        payload.put("generatedAt", Instant.now().toString());
        client.post()
                .uri(properties.getInsights())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    private record SessionResponse(Long id, Long customerId, String status) {
    }

    private record CustomerResponse(Long id, String name, String phone) {
    }

    private record MetricsResponse(Long customerId, BigDecimal totalSpend, int visitCount,
                                   BigDecimal avgOrderValue, String segment) {
    }
}
