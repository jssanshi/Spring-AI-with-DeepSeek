package com.example.chapter4.controller;

import com.example.chapter4.model.ChatMessage;
import com.example.chapter4.model.ChatThread;
import com.example.chapter4.service.ChatThreadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat/threads")
public class ChatThreadController {

    @Autowired
    private ChatThreadService chatService;

    @GetMapping
    public List<ChatThread> getAllThreads() {
        return chatService.getAllThreads();
    }

    @PostMapping
    public ChatThread saveThread(@RequestBody ChatThread thread) {
        return chatService.saveThread(thread);
    }

    @PostMapping(value = "/{threadId}/messages")
    public ChatMessage saveMessage(@PathVariable String threadId, @RequestBody ChatMessage chatMessage) {
        return chatService.saveMessage(threadId, chatMessage);
    }

    // 查询消息列表
    @GetMapping("/{threadId}/messages")
    public List<ChatMessage> getMessages(@PathVariable String threadId) {
        return chatService.getMessagesByThreadId(threadId);
    }
}
