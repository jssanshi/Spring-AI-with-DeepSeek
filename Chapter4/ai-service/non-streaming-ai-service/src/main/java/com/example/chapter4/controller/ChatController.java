package com.example.chapter4.controller;

import com.example.chapter4.service.ChatService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ChatController {
    private final ChatClient chatClient;
    private final ChatService chatService;

    public ChatController(@Qualifier("deepSeekChatClient") ChatClient chatClient, ChatService chatService) {
        this.chatClient = chatClient;
        this.chatService = chatService;
    }

    @GetMapping("/ai/generate")
    public String generate(@RequestParam(value = "message", defaultValue = "一句话介绍下Spring AI") String message) {
        return chatService.chatWithAdvisor(message);
    }

    @GetMapping(value = "/ai/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateStream(@RequestParam(value = "message", defaultValue = "一句话介绍下Spring AI") String message) {
        Flux<String> output = chatClient.prompt()
                .user(message)
                .stream()
                .content();

        return output;
    }
}