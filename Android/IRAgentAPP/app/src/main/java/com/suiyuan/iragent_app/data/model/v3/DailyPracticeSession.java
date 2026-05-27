package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DailyPracticeSession {
    @SerializedName("sessionId") private String sessionId;
    @SerializedName("questions") private List<PracticeQuestion> questions;
    @SerializedName("totalCount") private int totalCount;
    @SerializedName("estimatedTime") private int estimatedTime;

    public String getSessionId() { return sessionId; }
    public List<PracticeQuestion> getQuestions() { return questions; }
    public int getTotalCount() { return totalCount; }
    public int getEstimatedTime() { return estimatedTime; }
}
