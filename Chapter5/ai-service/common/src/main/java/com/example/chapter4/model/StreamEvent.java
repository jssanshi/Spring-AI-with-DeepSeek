package com.example.chapter4.model;

public class StreamEvent {
    private String type;
    private String id;
    private String delta;

    public StreamEvent(String type) {
        this.type = type;
    }

    public StreamEvent(String type, String id, String delta) {
        this.type = type;
        this.id = id;
        this.delta = delta;
    }

    public String getType() { return type; }
    public String getId() { return id; }
    public String getDelta() { return delta; }
}
