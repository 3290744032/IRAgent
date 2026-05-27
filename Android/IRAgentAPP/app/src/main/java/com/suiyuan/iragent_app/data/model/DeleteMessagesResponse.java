package com.suiyuan.iragent_app.data.model;

import com.google.gson.annotations.SerializedName;

public class DeleteMessagesResponse {
    @SerializedName("deletedCount")
    private int deletedCount;
    @SerializedName("conversationId")
    private String conversationId;
    @SerializedName("userId")
    private long userId;

    public int getDeletedCount() { return deletedCount; }
    public void setDeletedCount(int deletedCount) { this.deletedCount = deletedCount; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
}
