package com.example.chapter4.uitls;

import com.alibaba.fastjson2.JSONObject;
import com.example.chapter4.model.StreamEvent;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
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

        AtomicReference<String> currentType = new AtomicReference<>("text");
        StringBuilder buffer = new StringBuilder(); //关键：流式缓冲区

        Flux<String> mainStream = responseFlux.flatMap(chatResponse -> {
            lastMetadata.set(chatResponse.getMetadata());

            String token = chatResponse.getResult().getOutput().getText();

            if (token == null || token.isEmpty()) {
                return Flux.empty();
            }

            buffer.append(token);

            List<String> outputEvents = new ArrayList<>();

            while (true) {
                String buf = buffer.toString();

                //进入 reasoning
                if (currentType.get().equals("text")) {
                    int openIdx = buf.indexOf("<reasoning>");
                    if (openIdx != -1) {
                        String before = buf.substring(0, openIdx);
                        if (!before.isEmpty()) {
                            outputEvents.add(json("text-delta", before));
                        }
                        buffer.delete(0, openIdx + "<reasoning>".length());
                        currentType.set("reasoning");
                        continue;
                    }
                }

                //结束 reasoning
                if (currentType.get().equals("reasoning")) {
                    int closeIdx = buf.indexOf("</reasoning>");
                    if (closeIdx != -1) {
                        String reasoningText = buf.substring(0, closeIdx);
                        if (!reasoningText.isEmpty()) {
                            outputEvents.add(json("reasoning", reasoningText));
                        }
                        buffer.delete(0, closeIdx + "</reasoning>".length());
                        currentType.set("text");
                        continue;
                    }
                }

                //普通安全输出
                if (buffer.length() > 64) {
                    String safeChunk = buffer.substring(0, buffer.length() - 16);
                    buffer.delete(0, buffer.length() - 16);

                    outputEvents.add(
                            json(
                                    "reasoning".equals(currentType.get())
                                            ? "reasoning"
                                            : "text-delta",
                                    safeChunk
                            )
                    );
                    continue;
                }

                break;
            }

            if (outputEvents.isEmpty()) {
                return Flux.empty();
            }

            return Flux.fromIterable(outputEvents);
        });

        return Flux.concat(
                Flux.just(
                        JSONObject.toJSONString(new StreamEvent("start")),
                        JSONObject.toJSONString(new StreamEvent("start-step")),
                        JSONObject.toJSONString(new StreamEvent("text-start", textId, null))
                ),
                mainStream,
                Flux.defer(() -> {
                    //flush 剩余 buffer
                    if (buffer.length() > 0) {
                        String rest = buffer.toString();
                        buffer.setLength(0);

                        return Flux.just(
                                JSONObject.toJSONString(
                                        new StreamEvent(
                                                currentType.get().equals("reasoning") ? "reasoning" : "text-delta",
                                                textId,
                                                rest
                                        )
                                ),
                                JSONObject.toJSONString(new StreamEvent("finish"))
                        );
                    }

                    return Flux.just(JSONObject.toJSONString(new StreamEvent("finish")));
                })
        );
    }

    private String json(String type, String text) {
        return JSONObject.toJSONString(
                new StreamEvent(type, textId, text)
        );
    }

}
