package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SmartPaperRequest {
    @SerializedName("subject") private String subject;
    @SerializedName("examType") private String examType;
    @SerializedName("title") private String title;
    @SerializedName("questionCount") private int questionCount;
    @SerializedName("difficulty") private int difficulty;
    @SerializedName("knowledgePoints") private List<String> knowledgePoints;
    @SerializedName("excludeDone") private boolean excludeDone;

    public SmartPaperRequest(String subject, String examType, String title,
                             int questionCount, int difficulty,
                             List<String> knowledgePoints, boolean excludeDone) {
        this.subject = subject;
        this.examType = examType;
        this.title = title;
        this.questionCount = questionCount;
        this.difficulty = difficulty;
        this.knowledgePoints = knowledgePoints;
        this.excludeDone = excludeDone;
    }
}
