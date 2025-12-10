package com.example.chapter4.service;

public interface ChatMemoryService {
    String chat(String prompt, String userName);
    String vectorMemoryChat(String prompt, String userName);
    String graphMemoryChat(String prompt, String userName);
}
