package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.SerializedName;

public class GradingRequest {
    @SerializedName("content")
    private String content;
    @SerializedName("subjectType")
    private String subjectType;
    @SerializedName("maxScore")
    private int maxScore;

    public GradingRequest(String content, String subjectType, int maxScore) {
        this.content = content;
        this.subjectType = subjectType;
        this.maxScore = maxScore;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
    public int getMaxScore() { return maxScore; }
    public void setMaxScore(int maxScore) { this.maxScore = maxScore; }
}
