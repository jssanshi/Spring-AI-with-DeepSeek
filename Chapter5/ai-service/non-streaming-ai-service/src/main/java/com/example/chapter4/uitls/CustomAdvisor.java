package com.example.chapter4.uitls;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;

public class CustomAdvisor implements CallAdvisor, StreamAdvisor {
    private final static Logger logger = LoggerFactory.getLogger(CustomAdvisor.class);
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        Assert.notNull(chatClientRequest, "the chatClientRequest cannot be null");
        ChatClientRequest formattedChatClientRequest = augmentWithCustomInstructions(chatClientRequest);
        return callAdvisorChain.nextCall(formattedChatClientRequest);
    }
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        Assert.notNull(chatClientRequest, "the chatClientRequest cannot be null");
        ChatClientRequest formattedChatClientRequest = augmentWithCustomInstructions(chatClientRequest);
        return streamAdvisorChain.nextStream(formattedChatClientRequest);
    }
    private static ChatClientRequest augmentWithCustomInstructions(ChatClientRequest chatClientRequest) {
        String customInstructions = "以跟16岁小孩交流的方式做出响应，使用简单的词语和句子";
        Prompt augmentedPrompt = chatClientRequest.prompt()
                .augmentUserMessage(userMessage -> userMessage.mutate()
                        .text(userMessage.getText() + System.lineSeparator() + customInstructions)
                        .build());
        return ChatClientRequest.builder()
                .prompt(augmentedPrompt)
                .context(Map.copyOf(chatClientRequest.context()))
                .build();
    }
    @Override
    public String getName() {
        return "CustomAdvisor";
    }
    @Override
    public int getOrder() {
        return 0;
    }
}