package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GradingReport {
    @SerializedName("reportId")
    private String reportId;
    @SerializedName("totalScore")
    private int totalScore;
    @SerializedName("maxScore")
    private int maxScore;
    @SerializedName("correctCount")
    private int correctCount;
    @SerializedName("wrongCount")
    private int wrongCount;
    @SerializedName("accuracy")
    private double accuracy;
    @SerializedName("questions")
    private List<GradedQuestion> questions;

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }
    public int getMaxScore() { return maxScore; }
    public void setMaxScore(int maxScore) { this.maxScore = maxScore; }
    public int getCorrectCount() { return correctCount; }
    public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }
    public int getWrongCount() { return wrongCount; }
    public void setWrongCount(int wrongCount) { this.wrongCount = wrongCount; }
    public double getAccuracy() { return accuracy; }
    public void setAccuracy(double accuracy) { this.accuracy = accuracy; }
    public List<GradedQuestion> getQuestions() { return questions; }
    public void setQuestions(List<GradedQuestion> questions) { this.questions = questions; }
}
