package com.suiyuan.iragent_app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MessageListResponse {
    @SerializedName("conversationId")
    private String conversationId;
    @SerializedName("userId")
    private Long userId;
    @SerializedName("messages")
    private List<Message> messages;

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }
}
