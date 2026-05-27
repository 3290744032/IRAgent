package com.suiyuan.iragent_app.data.model.v2;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SessionSummaryResponse {
    @SerializedName("sessionId")
    private String sessionId;
    @SerializedName("topic")
    private String topic;
    @SerializedName("question")
    private String question;
    @SerializedName("totalTime")
    private String totalTime;
    @SerializedName("completedAt")
    private String completedAt;
    @SerializedName("knowledgeGraph")
    private KnowledgeGraph knowledgeGraph;
    @SerializedName("masterySummary")
    private MasterySummary masterySummary;
    @SerializedName("history")
    private List<HistoryItem> history;
    @SerializedName("recommendations")
    private List<Recommendation> recommendations;

    public String getSessionId() { return sessionId; }
    public String getTopic() { return topic; }
    public String getQuestion() { return question; }
    public String getTotalTime() { return totalTime; }
    public String getCompletedAt() { return completedAt; }
    public KnowledgeGraph getKnowledgeGraph() { return knowledgeGraph; }
    public MasterySummary getMasterySummary() { return masterySummary; }
    public List<HistoryItem> getHistory() { return history; }
    public List<Recommendation> getRecommendations() { return recommendations; }

    public static class HistoryItem {
        @SerializedName("stepIndex")
        private int stepIndex;
        @SerializedName("question")
        private String question;
        @SerializedName("userAnswer")
        private String userAnswer;
        @SerializedName("aiFeedback")
        private String aiFeedback;
        @SerializedName("attempts")
        private int attempts;

        public int getStepIndex() { return stepIndex; }
        public String getQuestion() { return question; }
        public String getUserAnswer() { return userAnswer; }
        public String getAiFeedback() { return aiFeedback; }
        public int getAttempts() { return attempts; }
    }
}
