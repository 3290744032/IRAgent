package com.suiyuan.iragent_app.data.model;

import com.google.gson.annotations.SerializedName;

public class Message {
    @SerializedName("messageId")
    private long messageId;
    @SerializedName("conversationId")
    private String conversationId;
    @SerializedName("senderType")
    private String senderType;
    @SerializedName("content")
    private String content;
    @SerializedName("messageType")
    private String messageType;
    @SerializedName("createdAt")
    private String createdAt;
    @SerializedName("status")
    private String status;

    public long getMessageId() { return messageId; }
    public void setMessageId(long messageId) { this.messageId = messageId; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getSenderType() { return senderType; }
    public void setSenderType(String senderType) { this.senderType = senderType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
