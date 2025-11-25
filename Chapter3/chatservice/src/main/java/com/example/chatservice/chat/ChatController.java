package com.example.chatservice.chat;

import com.alibaba.fastjson.JSONObject;
import com.example.chatservice.model.StreamEvent;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

@RestController
public class ChatController {

    private final DeepSeekChatModel chatModel;

    @Autowired
    public ChatController(DeepSeekChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping("/ai/generate")
    public Mono<Map<String, String>> generate(@RequestParam(value = "message", defaultValue = "一句话介绍下Spring AI") String message) {
        return Mono.fromCallable(() -> chatModel.call(message))
                .subscribeOn(Schedulers.boundedElastic())
                .map(generation -> Map.of("generation", generation));
    }

    @GetMapping(value = "/ai/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> generateStream(@RequestParam(value = "message", defaultValue = "介绍下Spring AI") String message) {
        var prompt = new Prompt(new UserMessage(message));
        return chatModel.stream(prompt);
    }

    @PostMapping(value = "/ai/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestBody JSONObject message) throws InterruptedException {
        var prompt = new Prompt(new UserMessage(message.toJSONString()));
        String textId = "txt-0";

        // 先发 start、start-step、text-start
        Flux<String> init = Flux.just(
                JSONObject.toJSONString(new StreamEvent("start")),
                JSONObject.toJSONString(new StreamEvent("start-step")),
                JSONObject.toJSONString(new StreamEvent("text-start", textId, null))//,
                //JSONObject.toJSONString(
                  //      new StreamEvent("text-delta", textId, "Spring AI 是一个基于 Spring 生态系统的框架，用于构建和集成人工智能应用，提供简洁的开发体验和强大的功能。" + (System.currentTimeMillis())))
        );

        // 把模型输出的 token 转成 text-delta
        Flux<String> deltas = chatModel.stream(prompt)
                .map(resp -> {
                    String token = resp.getResult().getOutput().getText();
                    return JSONObject.toJSONString(
                            new StreamEvent("text-delta", textId, token)
                    );
                });

        // 最后收尾：text-end, finish-step, finish, [DONE]
        Flux<String> ending = Flux.just(
                JSONObject.toJSONString(new StreamEvent("text-end", textId, null)),
                JSONObject.toJSONString(new StreamEvent("finish-step")),
                JSONObject.toJSONString(new StreamEvent("finish")),
                "[DONE]"
        );

        return Flux.concat(init, deltas, ending);
    }
}