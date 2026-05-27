package com.suiyuan.iragent_app.data.model.v2;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MasteryRecord {
    @SerializedName("knowledgePoint")
    private String knowledgePoint;
    @SerializedName("topic")
    private String topic;
    @SerializedName("proficiency")
    private int proficiency;
    @SerializedName("reviewCount")
    private int reviewCount;
    @SerializedName("lastReviewedAt")
    private String lastReviewedAt;
    @SerializedName("nextReviewAt")
    private String nextReviewAt;
    @SerializedName("misconceptions")
    private List<String> misconceptions;
    @SerializedName("status")
    private String status;

    public String getKnowledgePoint() { return knowledgePoint; }
    public String getTopic() { return topic; }
    public int getProficiency() { return proficiency; }
    public int getReviewCount() { return reviewCount; }
    public String getLastReviewedAt() { return lastReviewedAt; }
    public String getNextReviewAt() { return nextReviewAt; }
    public List<String> getMisconceptions() { return misconceptions; }
    public String getStatus() { return status; }
}
