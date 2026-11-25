package com.harmoni.pos.ai.adapter.in.web.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest().body(error(400, "VALIDATION_ERROR", "Request validation failed"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.badRequest().body(error(400, "INVALID_SESSION", e.getMessage()));
    }

    private static Map<String, Object> error(int status, String code, String message) {
        return Map.of("timestamp", Instant.now().toString(), "status", status,
                "code", code, "message", message);
    }
}
