package com.suiyuan.iragent_app.data.model.v2;

import com.google.gson.annotations.SerializedName;

public class SessionHistoryItem {
    @SerializedName("sessionId")
    private String sessionId;
    @SerializedName("topic")
    private String topic;
    @SerializedName("question")
    private String question;
    @SerializedName("subjectType")
    private String subjectType;
    @SerializedName("totalSteps")
    private int totalSteps;
    @SerializedName("completedSteps")
    private int completedSteps;
    @SerializedName("status")
    private String status;
    @SerializedName("overallProficiency")
    private Integer overallProficiency;
    @SerializedName("createdAt")
    private String createdAt;
    @SerializedName("completedAt")
    private String completedAt;

    public String getSessionId() { return sessionId; }
    public String getTopic() { return topic; }
    public String getQuestion() { return question; }
    public String getSubjectType() { return subjectType; }
    public int getTotalSteps() { return totalSteps; }
    public int getCompletedSteps() { return completedSteps; }
    public String getStatus() { return status; }
    public Integer getOverallProficiency() { return overallProficiency; }
    public String getCreatedAt() { return createdAt; }
    public String getCompletedAt() { return completedAt; }
}
