package com.example.promptaiservice;

import org.springframework.core.io.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {
    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    private static final String questionPromptTemplateStr = """
      问题回答，关于 {productType}: {question}
      """;

    private static final String questionPromptTemplateStr1 = """
        你是一个商品查询助手，如果你不知道相关商品信息，直接回复”不知道“。
        
        商品类型是：{productType}.
        
        用户问题是：{question}.
      """;

    @Value("classpath:/promptTemplates/questionPromptTemplate.st")
    Resource questionPromptTemplate;

    @PostMapping(value = "/ai/generate")
    public String generateWithPromptTemplate(@RequestBody Question question) {
        var answerText = chatClient.prompt()
                .user(userSpec -> userSpec
                        .text(questionPromptTemplate)
                        .param("productType", question.productType())
                        .param("question", question.question()))
                .call()
                .content();

        return answerText;
    }
}