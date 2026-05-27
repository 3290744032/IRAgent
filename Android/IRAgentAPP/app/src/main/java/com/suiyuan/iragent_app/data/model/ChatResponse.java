package com.suiyuan.iragent_app.data.model;

import com.google.gson.annotations.SerializedName;

public class ChatResponse {
    @SerializedName("response")
    private String response;
    @SerializedName("conversationId")
    private String conversationId;
    @SerializedName("userId")
    private long userId;

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
}
