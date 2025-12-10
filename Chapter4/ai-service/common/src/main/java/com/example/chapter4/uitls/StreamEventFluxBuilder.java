package com.example.chapter4.uitls;

import com.alibaba.fastjson2.JSONObject;
import com.example.chapter4.model.StreamEvent;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 通用流式事件构建器：
 * 1. 自动插入 start / finish 事件
 * 2. 支持流式 ChatResponse 映射为 text-delta
 * 3. 可选在 finish 事件中包含 metadata（token 使用信息）
 */
public class StreamEventFluxBuilder {

    private final String textId;
    private final boolean includeMetadataInFinish;

    /**
     * @param textId                 唯一文本标识
     * @param includeMetadataInFinish 是否在 "finish" 事件中附带 metadata
     */
    public StreamEventFluxBuilder(String textId, boolean includeMetadataInFinish) {
        this.textId = textId;
        this.includeMetadataInFinish = includeMetadataInFinish;
    }

    public Flux<String> build(Flux<ChatResponse> responseFlux) {
        AtomicReference<ChatResponseMetadata> lastMetadata = new AtomicReference<>();

        // 主流映射：ChatResponse -> text-delta JSON
        Flux<String> mainStream = responseFlux.map(chatResponse -> {
            lastMetadata.set(chatResponse.getMetadata());
            String token = chatResponse.getResult().getOutput().getText();
            return JSONObject.toJSONString(
                    new StreamEvent("text-delta", textId, token)
            );
        });

        // 拼接事件序列
        return Flux.concat(
                Flux.just(
                        JSONObject.toJSONString(new StreamEvent("start")),
                        JSONObject.toJSONString(new StreamEvent("start-step")),
                        JSONObject.toJSONString(new StreamEvent("text-start", textId, null))
                ),
                mainStream,
                Flux.defer(() -> {
                    ChatResponseMetadata metadata = lastMetadata.get();

                    // 打印 token 消耗日志
                    if (metadata != null && metadata.getUsage() != null) {
                        Usage usage = metadata.getUsage();
                        System.out.printf(
                                "[Chat] tokens used - prompt: %d, completion: %d, total: %d%n",
                                usage.getPromptTokens(),
                                usage.getCompletionTokens(),
                                usage.getTotalTokens()
                        );
                    }

                    // 构造 finish 事件
                    String finishEventJson;
                    if (includeMetadataInFinish && metadata != null && metadata.getUsage() != null) {
                        Usage usage = metadata.getUsage();
                        Map<String, Object> meta = new HashMap<>();
                        meta.put("promptTokens", usage.getPromptTokens());
                        meta.put("completionTokens", usage.getCompletionTokens());
                        meta.put("totalTokens", usage.getTotalTokens());

                        Map<String, Object> finishEvent = new HashMap<>();
                        finishEvent.put("event", "finish");
                        finishEvent.put("metadata", meta);
                        finishEventJson = JSONObject.toJSONString(finishEvent);
                    } else {
                        finishEventJson = JSONObject.toJSONString(new StreamEvent("finish"));
                    }

                    return Flux.just(
                            finishEventJson
                    );
                })
        );
    }
}
