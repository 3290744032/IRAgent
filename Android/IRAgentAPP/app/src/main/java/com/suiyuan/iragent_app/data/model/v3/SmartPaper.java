package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SmartPaper {
    @SerializedName("paperId") private String paperId;
    @SerializedName("title") private String title;
    @SerializedName("subject") private String subject;
    @SerializedName("examType") private String examType;
    @SerializedName("totalScore") private int totalScore;
    @SerializedName("questionCount") private int questionCount;
    @SerializedName("estimatedTime") private int estimatedTime;
    @SerializedName("questions") private List<PracticeQuestion> questions;

    public String getPaperId() { return paperId; }
    public String getTitle() { return title; }
    public String getSubject() { return subject; }
    public String getExamType() { return examType; }
    public int getTotalScore() { return totalScore; }
    public int getQuestionCount() { return questionCount; }
    public int getEstimatedTime() { return estimatedTime; }
    public List<PracticeQuestion> getQuestions() { return questions; }
}
