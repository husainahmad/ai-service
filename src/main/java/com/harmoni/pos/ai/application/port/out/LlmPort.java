package com.harmoni.pos.ai.application.port.out;

import reactor.core.publisher.Flux;

public interface LlmPort {

    String generate(long sessionId, String prompt);

    Flux<String> stream(long sessionId, String prompt);
}
