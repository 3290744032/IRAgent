package com.suiyuan.iragent_app.data.model;

import com.google.gson.annotations.SerializedName;

public class ChatRequest {
    @SerializedName("message")
    private String message;
    @SerializedName("conversationId")
    private String conversationId;

    public ChatRequest(String message, String conversationId) {
        this.message = message;
        this.conversationId = conversationId;
    }
}
