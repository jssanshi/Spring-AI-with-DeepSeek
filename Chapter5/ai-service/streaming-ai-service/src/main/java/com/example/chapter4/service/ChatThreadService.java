package com.example.chapter4.service;



import com.example.chapter4.model.ChatMessage;
import com.example.chapter4.model.ChatThread;

import java.util.List;

public interface ChatThreadService {
    List<ChatThread> getAllThreads(String userName);
    ChatThread saveThread(ChatThread chatThread);
    ChatMessage saveMessage(String threadId, ChatMessage chatMessage, String userName);
    List<ChatMessage> getMessagesByThreadId(String threadId);
}
