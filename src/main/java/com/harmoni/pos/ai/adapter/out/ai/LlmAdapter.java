package com.harmoni.pos.ai.adapter.out.ai;

import com.harmoni.pos.ai.application.port.out.LlmPort;
import com.harmoni.pos.ai.application.service.PageToolSelector;
import com.harmoni.pos.ai.config.AiProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LlmAdapter implements LlmPort {

    private static final String DISABLED = "AI assistant is currently disabled.";

    private final ChatClient dashboardChatClient;
    private final AiProperties properties;
    private final PageToolSelector pageTools;

    public LlmAdapter(@Qualifier("dashboardChatClient") ChatClient dashboardChatClient,
                      AiProperties properties,
                      PageToolSelector pageTools) {
        this.dashboardChatClient = dashboardChatClient;
        this.properties = properties;
        this.pageTools = pageTools;
    }

    @Override
    public String generateWithoutMemory(String prompt, String toolName) {
        if (!properties.isEnabled()) {
            return DISABLED;
        }
        ChatClient.ChatClientRequestSpec request = dashboardChatClient.prompt();
        List<ToolCallback> tools = pageTools.toolsFor(toolName);
        if (!tools.isEmpty()) {
            request = request.toolCallbacks(tools);
        }
        String reply = request.user(prompt).call().content();
        return reply == null || reply.isBlank() ? "I could not generate a reply. Please try again." : reply;
    }
}
