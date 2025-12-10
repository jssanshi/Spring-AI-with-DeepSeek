package com.example.chapter4.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "chat_message")
public class ChatMessage {
    @Id
    private String id;

    @Field(name = "message_id")
    private String messageId;

    private String role;
    private String type;
    private String content;

    @Field(name = "parent_id")
    private String parentId;

    @Field(name = "thread_id")
    private String threadId;

    @Field(name = "created_at")
    private String createdAt;

    @Field(name = "user_name")
    private String userName;
}
