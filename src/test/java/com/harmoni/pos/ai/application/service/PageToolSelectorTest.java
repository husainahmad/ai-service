package com.harmoni.pos.ai.application.service;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PageToolSelectorTest {

    @Test
    void returnsOnlyTheRequestedTool() {
        ToolCallback sales = callback("pos_get_sales_report");
        ToolCallback products = callback("pos_search_products");
        PageToolSelector selector = selector(List.of(sales, products));

        assertThat(selector.toolsFor("pos_get_sales_report")).containsExactly(sales);
        assertThat(selector.toolsFor("pos_search_products")).containsExactly(products);
    }

    @Test
    void matchesToolNamesWithMcpClientPrefix() {
        ToolCallback sales = callback("JavaSDKMCPClient_pos_get_sales_report");
        PageToolSelector selector = selector(List.of(sales));

        assertThat(selector.toolsFor("pos_get_sales_report")).containsExactly(sales);
    }

    @Test
    void returnsNoToolsForBlankOrUnknownName() {
        PageToolSelector selector = selector(List.of(callback("pos_get_sales_report")));

        assertThat(selector.toolsFor(null)).isEmpty();
        assertThat(selector.toolsFor(" ")).isEmpty();
        assertThat(selector.toolsFor("pos_missing")).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void loadsTheToolListOnlyOnce() {
        ToolCallbackProvider callbackProvider =
                () -> new ToolCallback[]{callback("pos_get_sales_report")};
        ObjectProvider<ToolCallbackProvider> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(callbackProvider);
        PageToolSelector selector = new PageToolSelector(provider);

        selector.toolsFor("pos_get_sales_report");
        selector.toolsFor("pos_get_sales_report");

        verify(provider, times(1)).getIfAvailable();
    }

    @SuppressWarnings("unchecked")
    private static PageToolSelector selector(List<ToolCallback> callbacks) {
        ToolCallbackProvider callbackProvider = () -> callbacks.toArray(ToolCallback[]::new);
        ObjectProvider<ToolCallbackProvider> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(callbackProvider);
        return new PageToolSelector(provider);
    }

    private static ToolCallback callback(String name) {
        ToolCallback callback = mock(ToolCallback.class);
        when(callback.getToolDefinition()).thenReturn(ToolDefinition.builder()
                .name(name)
                .description("test")
                .inputSchema("{}")
                .build());
        return callback;
    }
}
