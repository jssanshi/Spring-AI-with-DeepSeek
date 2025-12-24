package com.example.promptaiservice_2;

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

    @Value("classpath:/promptTemplates/systemPromptTemplate.st")
    Resource promptTemplate;

    @PostMapping(value = "/ai/generate")
    public String generateWithPromptTemplate(@RequestBody Question question) {
        var products = productsDbUtils.getProducts();

        var answerText = chatClient.prompt()
                .system(systemSpec -> systemSpec
                        .text(promptTemplate)
                        .param("productType", question.productType())
                        .param("products", products))
                .user(question.question())
                .call()
                .content();

        return answerText;
    }
}