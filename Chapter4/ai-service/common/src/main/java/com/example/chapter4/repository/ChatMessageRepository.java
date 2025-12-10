package com.example.chapter4.repository;

import com.example.chapter4.model.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findByThreadId(String thread_id);
}
