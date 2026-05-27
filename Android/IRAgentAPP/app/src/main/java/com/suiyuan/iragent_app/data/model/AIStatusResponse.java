package com.suiyuan.iragent_app.data.model;

import com.google.gson.annotations.SerializedName;

public class AIStatusResponse {
    @SerializedName("status")
    private String status;
    @SerializedName("testResponse")
    private String testResponse;
    @SerializedName("redis")
    private String redis;
    @SerializedName("sessionStats")
    private SessionStats sessionStats;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTestResponse() { return testResponse; }
    public void setTestResponse(String testResponse) { this.testResponse = testResponse; }
    public String getRedis() { return redis; }
    public void setRedis(String redis) { this.redis = redis; }
    public SessionStats getSessionStats() { return sessionStats; }
    public void setSessionStats(SessionStats sessionStats) { this.sessionStats = sessionStats; }
}
