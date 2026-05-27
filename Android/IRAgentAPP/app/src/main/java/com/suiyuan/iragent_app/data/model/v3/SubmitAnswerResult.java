package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SubmitAnswerResult {
    @SerializedName("totalCount") private int totalCount;
    @SerializedName("correctCount") private int correctCount;
    @SerializedName("wrongCount") private int wrongCount;
    @SerializedName("accuracy") private double accuracy;
    @SerializedName("totalTime") private int totalTime;
    @SerializedName("details") private List<AnswerDetail> details;

    public int getTotalCount() { return totalCount; }
    public int getCorrectCount() { return correctCount; }
    public int getWrongCount() { return wrongCount; }
    public double getAccuracy() { return accuracy; }
    public int getTotalTime() { return totalTime; }
    public List<AnswerDetail> getDetails() { return details; }

    public static class AnswerDetail {
        @SerializedName("questionId") private String questionId;
        @SerializedName("isCorrect") private boolean isCorrect;
        @SerializedName("selectedAnswer") private String selectedAnswer;
        @SerializedName("correctAnswer") private String correctAnswer;
        @SerializedName("explanation") private String explanation;

        public String getQuestionId() { return questionId; }
        public boolean isCorrect() { return isCorrect; }
        public String getSelectedAnswer() { return selectedAnswer; }
        public String getCorrectAnswer() { return correctAnswer; }
        public String getExplanation() { return explanation; }
    }
}
