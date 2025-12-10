package com.example.chapter4.service.impl;

import com.example.chapter4.model.ActorFilms;
import com.example.chapter4.service.ChatService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;


@Component
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;

    private final ChatClient openAICompatibleChatClient;

    public ChatServiceImpl(@Qualifier("deepSeekChatClient") ChatClient chatClient,
                           @Qualifier("openAICompatibleChatClient") ChatClient openAICompatibleChatClient) {
        this.chatClient = chatClient;
        this.openAICompatibleChatClient = openAICompatibleChatClient;
    }

    @Override
    public ActorFilms chat(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .call()
                .entity(ActorFilms.class);
    }

    @Override
    public String chatWithAdvisor(String prompt) {
        return openAICompatibleChatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
