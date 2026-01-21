package com.example.chapter4.service.impl;

import com.example.chapter4.service.ChatMemoryService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;


@Component
public class ChatMemoryServiceImpl implements ChatMemoryService {

    private final ChatClient chatClient;
    private final ChatClient vectorMemoryChatClient;
    private final ChatClient graphMemoryChatClient;

    public ChatMemoryServiceImpl(@Qualifier("deepSeekChatClient") ChatClient chatClient,
                                 @Qualifier("ollamaChatClient") ChatClient vectorMemoryChatClient,
                                 @Qualifier("deepSeekGraphChatClient") ChatClient graphMemoryChatClient) {
        this.chatClient = chatClient;
        this.vectorMemoryChatClient = vectorMemoryChatClient;
        this.graphMemoryChatClient = graphMemoryChatClient;
    }

    public String chat(String prompt, String userName) {
        return chatClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userName))
                .call()
                .content();
    }

    public String vectorMemoryChat(String prompt, String userName) {
        return vectorMemoryChatClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userName))
                .call()
                .content();
    }

    public String graphMemoryChat(String prompt, String userName) {
        return graphMemoryChatClient.prompt("以普通用户身份做出回复，不要以程序员身份")
                .user(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userName))
                .call()
                .content();
    }
}
