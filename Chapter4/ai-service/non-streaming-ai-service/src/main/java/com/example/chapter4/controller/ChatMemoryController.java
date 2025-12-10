package com.example.chapter4.controller;

import com.example.chapter4.service.ChatMemoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat/memory")
public class ChatMemoryController {
    private final ChatMemoryService chatMemoryService;

    public ChatMemoryController(ChatMemoryService chatMemoryService) {
        this.chatMemoryService = chatMemoryService;
    }

    @GetMapping
    public String chat(@RequestParam(value = "message", defaultValue = "一句话介绍下Spring AI") String message, @RequestParam(value = "userName") String userName) {
        return chatMemoryService.chat(message, userName);
    }

    @GetMapping("/vectorMemory")
    public String vectorMemoryChat(@RequestParam(value = "message", defaultValue = "一句话介绍下Spring AI") String message, @RequestParam(value = "userName") String userName) {
        return chatMemoryService.vectorMemoryChat(message, userName);
    }

    @GetMapping("/graphMemory")
    public String graphMemoryChat(@RequestParam(value = "message", defaultValue = "一句话介绍下Spring AI") String message, @RequestParam(value = "userName") String userName) {
        return chatMemoryService.graphMemoryChat(message, userName);
    }
}
