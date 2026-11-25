package com.harmoni.pos.ai.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PageToolSelector {

    private final ObjectProvider<ToolCallbackProvider> providers;

    private volatile Map<String, ToolCallback> callbacks;

    public List<ToolCallback> toolsFor(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            return List.of();
        }
        for (ToolCallback callback : callbacks().values()) {
            String callbackName = callback.getToolDefinition().name();
            if (callbackName.equals(toolName) || callbackName.endsWith("_" + toolName)) {
                log.debug("Attaching page tool '{}' as '{}'", toolName, callbackName);
                return List.of(callback);
            }
        }
        log.warn("Page tool '{}' is not available from MCP; sending no tools", toolName);
        return List.of();
    }

    private Map<String, ToolCallback> callbacks() {
        Map<String, ToolCallback> cached = callbacks;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (callbacks == null) {
                callbacks = loadCallbacks();
            }
            return callbacks;
        }
    }

    private Map<String, ToolCallback> loadCallbacks() {
        ToolCallbackProvider provider = providers.getIfAvailable();
        if (provider == null) {
            log.warn("No MCP ToolCallbackProvider is available; page tools are disabled");
            return Map.of();
        }
        Map<String, ToolCallback> loaded = Arrays.stream(provider.getToolCallbacks())
                .collect(Collectors.toUnmodifiableMap(
                        callback -> callback.getToolDefinition().name(),
                        Function.identity(),
                        (left, right) -> left));
        log.info("Loaded {} MCP tool callbacks for page tool selection", loaded.size());
        return loaded;
    }
}
