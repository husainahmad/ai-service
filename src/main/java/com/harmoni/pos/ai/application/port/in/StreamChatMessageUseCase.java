package com.harmoni.pos.ai.application.port.in;

import reactor.core.publisher.Flux;

public interface StreamChatMessageUseCase {

    Flux<String> stream(StreamChatMessageCommand command);
}
