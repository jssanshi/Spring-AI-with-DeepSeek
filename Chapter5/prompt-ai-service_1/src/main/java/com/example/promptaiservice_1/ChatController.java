package com.example.promptaiservice_1;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {
    private final ChatClient chatClient;
    private final ProductsDbUtils productsDbUtils;

    public ChatController(ChatClient.Builder chatClientBuilder, ProductsDbUtils productsDbUtils) {
        this.chatClient = chatClientBuilder.build();
        this.productsDbUtils = productsDbUtils;
    }

    @Value("classpath:/promptTemplates/questionPromptTemplate.st")
    Resource questionPromptTemplate;

    @PostMapping(value = "/ai/generate")
    public String generateWithPromptTemplate(@RequestBody Question question) {
        var products = productsDbUtils.getProducts();

        var answerText = chatClient.prompt()
                .user(userSpec -> userSpec
                        .text(questionPromptTemplate)
                        .param("productType", question.productType())
                        .param("question", question.question())
                        .param("products", products))
                .call()
                .content();

        return answerText;
    }
}