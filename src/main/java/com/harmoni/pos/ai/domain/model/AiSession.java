package com.harmoni.pos.ai.domain.model;

public class AiSession {

    private final Long id;
    private final Long customerId;
    private final String status;

    public AiSession(Long id, Long customerId, String status) {
        this.id = id;
        this.customerId = customerId;
        this.status = status;
    }

    public boolean isOpen() {
        return "OPEN".equalsIgnoreCase(status);
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getStatus() {
        return status;
    }
}
