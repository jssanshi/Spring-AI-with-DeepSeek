package com.example.chapter4.service.impl;

import com.example.chapter4.model.ChatMessage;
import com.example.chapter4.model.ChatThread;
import com.example.chapter4.repository.ChatMessageRepository;
import com.example.chapter4.repository.ChatThreadsRepository;
import com.example.chapter4.service.ChatThreadService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChatThreadServiceImpl implements ChatThreadService {

    private ChatThreadsRepository chatThreadsRepository;

    private ChatMessageRepository chatMessageRepository;

    public ChatThreadServiceImpl(ChatThreadsRepository chatThreadsRepository, ChatMessageRepository chatMessageRepository) {
        this.chatThreadsRepository = chatThreadsRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    public List<ChatThread> getAllThreads() {
        return chatThreadsRepository.findAll();
    }

    public ChatThread saveThread(ChatThread chatThread) {
        return chatThreadsRepository.save(chatThread);
    }

    @Override
    public ChatMessage saveMessage(String threadId, ChatMessage chatMessage) {
        chatMessage.setThreadId(threadId);
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        String formattedTime = now.format(formatter);
        chatMessage.setCreatedAt(formattedTime);

        return chatMessageRepository.save(chatMessage);
    }

    public List<ChatMessage> getMessagesByThreadId(String threadId) {
        List<ChatMessage> all = chatMessageRepository.findByThreadId(threadId);

        if (all.isEmpty()) return List.of();

        // 找到起始消息（没有 parentId）
        ChatMessage root = all.stream()
                .filter(m -> m.getContent() == null)
                .findFirst()
                .orElse(all.get(0));

        // 按 parent_id 串联
        List<ChatMessage> ordered = new ArrayList<>();
        ChatMessage current = root;
        while (current != null) {
            ordered.add(current);
            // 找下一个以当前 id 为 parentId 的消息
            ChatMessage finalCurrent = current;
            ChatMessage next = all.stream()
                    .filter(m -> finalCurrent.getMessageId().equals(m.getParentId()))
                    .findFirst()
                    .orElse(null);

            current = next;
        }

        return ordered;
    }
}
