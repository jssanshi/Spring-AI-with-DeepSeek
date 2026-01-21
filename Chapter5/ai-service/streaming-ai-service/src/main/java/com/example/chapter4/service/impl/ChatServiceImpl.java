package com.example.chapter4.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.chapter4.service.ChatService;
import com.example.chapter4.uitls.CustomSimpleVectorStore;
import com.example.chapter4.uitls.ProductsDbUtils;
import com.example.chapter4.uitls.StreamEventFluxBuilder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import com.example.chapter4.model.StreamEvent;

import java.util.ArrayList;
import java.util.List;


@Service
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;

    private final CustomSimpleVectorStore vectorStore;

    private final ProductsDbUtils productsDbUtils;

    public ChatServiceImpl(@Qualifier("deepSeekChatClient") ChatClient chatClient, CustomSimpleVectorStore vectorStore, ProductsDbUtils productsDbUtils) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
        this.productsDbUtils = productsDbUtils;
    }

    @Value("classpath:/promptTemplates/questionPromptTemplate.st")
    Resource questionPromptTemplate;

    @Override
    public Flux<String> chat(String message, String userName) {
        List<Document> memList = new ArrayList<>();

        // 设置回调
        vectorStore.setMemoryListener(memList::addAll);

        String textId = "txt-0";
        var products = productsDbUtils.getProducts();
        return chatClient
                .prompt()
                .user(userSpec -> userSpec
                        .text(questionPromptTemplate)
                        .param("question", message)
                        .param("products", products))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userName))
                .stream()
                .chatResponse()
                .transform(stream -> stream.contextWrite(ctx -> ctx.put("memList", memList)))
                .doFinally(s -> vectorStore.setMemoryListener(null))
                .transform(resp ->
                        new StreamEventFluxBuilder(textId, true).build(resp)
                )
                .concatWith(
                        Flux.defer(() -> {
                            if (memList.isEmpty()) {
                                return Flux.empty();
                            }
                            return Flux.just(buildMemoryJson(textId, memList, userName));
                        })
                ).concatWith(
                        Flux.just(
                            com.alibaba.fastjson2.JSONObject.toJSONString(new StreamEvent("text-end", textId, null)),
                            com.alibaba.fastjson2.JSONObject.toJSONString(new StreamEvent("finish-step")),
                            "[DONE]"
                    )
                );
    }

    private String buildMemoryJson(String textId, List<Document> memList, String userId) {
        JSONObject anno = new JSONObject();
        anno.put("type", "mem");
        anno.put("id", textId);

        // 创建delta对象
        JSONObject delta = new JSONObject();
        delta.put("type", "mem0-get");

        // 创建memories数组
        JSONArray memoriesArray = new JSONArray();
        for (Document doc : memList) {
            // 提取text内容
            String textContent = extractTextFromMemory(doc.getText());

            JSONObject memoryObj = new JSONObject();
            memoryObj.put("id", doc.getId());
            memoryObj.put("memory", textContent);
            memoryObj.put("user_id", userId);
            memoryObj.put("score", doc.getScore());
            memoriesArray.add(memoryObj);
        }

        delta.put("memories", memoriesArray);
        anno.put("delta", delta);

        return anno.toString();
    }

    /**
     * 从memory JSON字符串中提取text内容
     */
    private String extractTextFromMemory(String memoryJson) {
        try {
            // 先解析外层的memory JSON字符串
            JSONObject memoryObj = JSONObject.parseObject(memoryJson);

            // 获取baseMessages数组
            JSONArray baseMessages = memoryObj.getJSONArray("baseMessages");
            if (baseMessages != null && !baseMessages.isEmpty()) {
                // 获取第一个baseMessage
                JSONObject firstMessage = baseMessages.getJSONObject(0);
                JSONArray contentArray = firstMessage.getJSONArray("content");

                if (contentArray != null && !contentArray.isEmpty()) {
                    // 获取第一个content对象
                    JSONObject firstContent = contentArray.getJSONObject(0);
                    return firstContent.getString("text");
                }
            }
        } catch (Exception e) {
            // 如果解析失败，返回原始字符串
            return memoryJson;
        }

        return memoryJson;
    }
}
