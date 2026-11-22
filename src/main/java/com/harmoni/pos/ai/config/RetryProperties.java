package com.harmoni.pos.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Data
@ConfigurationProperties(prefix = "harmoni.retry")
public class RetryProperties {

    private int maxAttempts = 3;
    private Duration initialBackoff = Duration.ofMillis(500);
    private Duration maxBackoff = Duration.ofSeconds(5);
    private double multiplier = 2.0;
}
