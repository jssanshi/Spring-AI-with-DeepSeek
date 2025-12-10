package com.example.chapter4.service;

import com.example.chapter4.model.ChatMessage;
import com.example.chapter4.model.ChatThread;

import java.util.List;

public interface ChatThreadService {
    List<ChatThread> getAllThreads();
    ChatThread saveThread(ChatThread chatThread);
    ChatMessage saveMessage(String threadId, ChatMessage chatMessage);
    List<ChatMessage> getMessagesByThreadId(String threadId);
}
