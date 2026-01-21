package com.example.chapter4.uitls;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    public CustomSimpleVectorStore customStore(@Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel) {
        return new CustomSimpleVectorStore(embeddingModel, 0.65);
    }

    @Bean
    public ChatClient deepSeekChatClient(DeepSeekChatModel chatModel, CustomSimpleVectorStore  vectorStore ) {
        VectorStoreChatMemoryAdvisor vectorStoreChatMemoryAdvisor = VectorStoreChatMemoryAdvisor.builder(vectorStore)
                .defaultTopK(10)
                .build();

        return ChatClient.builder(chatModel)
                .defaultSystem("使用中文进行回复，以普通用户身份做出回复，不要以程序员身份")
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        vectorStoreChatMemoryAdvisor
                )
                .build();
    }
}
