package com.harmoni.pos.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "harmoni.services.customer")
public class CustomerServiceProperties {

    private String baseUrl = "http://localhost:8084";
    private String session = "/api/v1/customer-sessions/{sessionId}";
    private String messages = "/api/v1/customer-sessions/{sessionId}/messages";
    private String customer = "/api/v1/customers/{customerId}";
    private String metrics = "/api/v1/growth/customers/{customerId}/metrics";
    private String insights = "/internal/v1/insights";
}
