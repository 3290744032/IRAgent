package com.suiyuan.iragent_app.data.model;

import com.google.gson.annotations.SerializedName;

public class TimelineTitleResponse {
    @SerializedName("title")
    private String title;

    @SerializedName("saved")
    private boolean saved;

    @SerializedName("conversationId")
    private String conversationId;

    public String getTitle() { return title; }
    public boolean isSaved() { return saved; }
    public String getConversationId() { return conversationId; }
}
