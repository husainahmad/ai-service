package com.harmoni.pos.ai.config;

import io.netty.channel.ChannelOption;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient dashboardChatClient(OpenAiChatModel model, AiProperties properties) {
        return ChatClient.builder(model)
                .defaultSystem(properties.getPageSystemPrompt())
                .build();
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                .responseTimeout(Duration.ofSeconds(180));
    }

    @Bean
    public RestClient.Builder restClientBuilder() {
        java.net.http.HttpClient jdkHttpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdkHttpClient);
        factory.setReadTimeout(Duration.ofSeconds(180));
        return RestClient.builder().requestFactory(factory);
    }
}
