package com.harmoni.pos.ai.application.port.out;

public interface LlmPort {

    String generateWithoutMemory(String prompt, String toolName);
}
