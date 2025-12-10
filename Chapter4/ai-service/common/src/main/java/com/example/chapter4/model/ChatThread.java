package com.example.chapter4.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "chat_thread")
public class ChatThread {
    @Id
    private String id;
    private String status;
    private String title;
    @Field(name = "user_name")
    private String userName;
}
