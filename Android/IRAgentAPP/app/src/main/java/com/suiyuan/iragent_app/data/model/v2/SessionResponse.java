package com.suiyuan.iragent_app.data.model.v2;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SessionResponse {
    @SerializedName("sessionId")
    private String sessionId;
    @SerializedName("question")
    private String question;
    @SerializedName("topic")
    private String topic;
    @SerializedName("subjectType")
    private String subjectType;
    @SerializedName("totalSteps")
    private int totalSteps;
    @SerializedName("currentStep")
    private int currentStep;
    @SerializedName("status")
    private String status;
    @SerializedName("createdAt")
    private String createdAt;
    @SerializedName("completedAt")
    private String completedAt;
    @SerializedName("steps")
    private List<LearningStep> steps;

    public String getSessionId() { return sessionId; }
    public String getQuestion() { return question; }
    public String getTopic() { return topic; }
    public String getSubjectType() { return subjectType; }
    public int getTotalSteps() { return totalSteps; }
    public int getCurrentStep() { return currentStep; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
    public String getCompletedAt() { return completedAt; }
    public List<LearningStep> getSteps() { return steps; }

    public boolean isInProgress() {
        return "in_progress".equals(status);
    }

    public boolean isCompleted() {
        return "completed".equals(status);
    }
}
