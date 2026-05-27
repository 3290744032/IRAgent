package com.suiyuan.iragent_app.data.model;

import com.google.gson.annotations.SerializedName;

public class DeleteConversationResponse {
    @SerializedName("success")
    private boolean success;
    @SerializedName("userId")
    private long userId;
    @SerializedName("conversationId")
    private String conversationId;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
}
