package com.example.chapter4.repository;

import com.example.chapter4.model.ChatThread;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatThreadsRepository extends MongoRepository<ChatThread, String> {
    List<ChatThread> findByUserName(String userName);
}
