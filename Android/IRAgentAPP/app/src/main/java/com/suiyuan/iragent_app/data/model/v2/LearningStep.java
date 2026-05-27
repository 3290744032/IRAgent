package com.suiyuan.iragent_app.data.model.v2;

import com.google.gson.annotations.SerializedName;

public class LearningStep {
    @SerializedName("stepId")
    private int stepId;
    @SerializedName("index")
    private int index;
    @SerializedName("title")
    private String title;
    @SerializedName("content")
    private String content;
    @SerializedName("question")
    private String question;
    @SerializedName("hint")
    private String hint;
    @SerializedName("status")
    private String status;
    @SerializedName("masteredAt")
    private String masteredAt;
    @SerializedName("knowledgePoint")
    private String knowledgePoint;
    @SerializedName("userAnswer")
    private String userAnswer;
    @SerializedName("aiFeedback")
    private String aiFeedback;
    @SerializedName("attempts")
    private int attempts;
    @SerializedName("answeredAt")
    private String answeredAt;
    @SerializedName("explanation")
    private String explanation;
    @SerializedName("example")
    private String example;

    public int getStepId() { return stepId; }
    public int getIndex() { return index; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getQuestion() { return question; }
    public String getHint() { return hint; }
    public String getStatus() { return status; }
    public String getMasteredAt() { return masteredAt; }
    public String getKnowledgePoint() { return knowledgePoint; }
    public String getUserAnswer() { return userAnswer; }
    public String getAiFeedback() { return aiFeedback; }
    public int getAttempts() { return attempts; }
    public String getAnsweredAt() { return answeredAt; }
    public String getExplanation() { return explanation; }
    public String getExample() { return example; }
}
