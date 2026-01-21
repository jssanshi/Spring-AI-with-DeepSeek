package com.example.chapter4.uitls;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.MysqlChatMemoryRepositoryDialect;
import org.springframework.ai.chat.memory.repository.neo4j.Neo4jChatMemoryRepository;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@Configuration
public class ChatClientConfig {
    @Bean
    public ChatClient deepSeekChatClient(DeepSeekChatModel chatModel, ChatMemory chatMemory, JdbcTemplate jdbcTemplate) {
        ChatMemoryRepository chatMemoryRepository = JdbcChatMemoryRepository.builder()
                .jdbcTemplate(jdbcTemplate)
                .dialect(new MysqlChatMemoryRepositoryDialect())
                .build();
        chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(10)
                .build();
        return ChatClient.builder(chatModel)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor() // 输出聊天日志
                )
                .build();
    }

    @Bean
    public ChatClient ollamaChatClient(DeepSeekChatModel chatModel, CustomSimpleVectorStore  vectorStore ) {
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

    @Bean
    public ChatClient deepSeekGraphChatClient(DeepSeekChatModel chatModel, Neo4jChatMemoryRepository chatMemoryRepository) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(10)
                .build();
        return ChatClient.builder(chatModel)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor() // 输出聊天日志
                )
                .build();
    }

    List<String> forbiddenWords = List.of("词汇1");
    SafeGuardAdvisor safeGuardAdvisor = new SafeGuardAdvisor(forbiddenWords);

    @Bean
    public ChatClient openAICompatibleChatClient(OpenAiChatModel baseChatModel) {
        return ChatClient.builder(baseChatModel)
                .defaultAdvisors(
                        safeGuardAdvisor
                )
                .build();
    }
}
