package com.suiyuan.iragent_app.data.model;

import com.google.gson.annotations.SerializedName;

public class ConversationListResponse {
    @SerializedName("conversation")
    private Conversation conversation;
    @SerializedName("userId")
    private Long userId;

    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
