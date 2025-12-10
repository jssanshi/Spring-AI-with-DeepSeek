package com.example.chapter4.model;

import com.alibaba.fastjson.JSONObject;

public class StreamAnnEvent {
    private String type;
    private String id;
    private JSONObject delta;

    public StreamAnnEvent(String type, String id, JSONObject delta) {
        this.type = type;
        this.id = id;
        this.delta = delta;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public JSONObject getDelta() {
        return delta;
    }
}
