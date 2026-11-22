package com.harmoni.pos.ai.domain.exception;

public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(long sessionId) {
        super("Customer session not found: " + sessionId);
    }
}
