package com.example.chapter4.service;

import com.example.chapter4.model.ActorFilms;

public interface ChatService {
    ActorFilms chat(String prompt);
    String chatWithAdvisor(String prompt);
}
