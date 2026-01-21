package com.example.chapter4.service;

import reactor.core.publisher.Flux;

public interface ChatService {
    Flux<String> chat(String prompt, String userName);
}
