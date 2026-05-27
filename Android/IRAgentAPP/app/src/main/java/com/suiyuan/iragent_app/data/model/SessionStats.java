package com.suiyuan.iragent_app.data.model;

import com.google.gson.annotations.SerializedName;

public class SessionStats {
    @SerializedName("totalConversations")
    private int totalConversations;
    @SerializedName("activeSessions")
    private int activeSessions;

    public int getTotalConversations() { return totalConversations; }
    public void setTotalConversations(int totalConversations) { this.totalConversations = totalConversations; }
    public int getActiveSessions() { return activeSessions; }
    public void setActiveSessions(int activeSessions) { this.activeSessions = activeSessions; }
}
