package com.suiyuan.iragent_app.data.model;

import com.google.gson.annotations.SerializedName;

public class GenerateTimelineRequest {
    @SerializedName("topic")
    private String topic;

    @SerializedName("conversationId")
    private String conversationId;

    public GenerateTimelineRequest(String topic) {
        this.topic = topic;
    }

    public GenerateTimelineRequest(String topic, String conversationId) {
        this.topic = topic;
        this.conversationId = conversationId;
    }
}
