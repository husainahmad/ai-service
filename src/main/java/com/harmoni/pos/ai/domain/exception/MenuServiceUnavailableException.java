package com.harmoni.pos.ai.domain.exception;

public class MenuServiceUnavailableException extends RuntimeException {

    public MenuServiceUnavailableException(String message) {
        super(message);
    }

    public MenuServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
