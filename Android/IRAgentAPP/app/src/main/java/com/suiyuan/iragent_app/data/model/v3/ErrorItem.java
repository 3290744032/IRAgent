package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.SerializedName;

public class ErrorItem {
    @SerializedName("id")
    private String id;
    @SerializedName("question_text")
    private String questionText;
    @SerializedName("student_answer")
    private String studentAnswer;
    @SerializedName("correct_answer")
    private String correctAnswer;
    @SerializedName("knowledge_point")
    private String knowledgePoint;
    @SerializedName("error_type")
    private String errorType;
    @SerializedName("subject")
    private String subject;
    @SerializedName("review_level")
    private int reviewLevel;
    @SerializedName("mastered")
    private boolean mastered;
    @SerializedName("created_at")
    private String createdAt;
    @SerializedName("reviewed_at")
    private String reviewedAt;
    @SerializedName("next_review_at")
    private String nextReviewAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getStudentAnswer() { return studentAnswer; }
    public void setStudentAnswer(String studentAnswer) { this.studentAnswer = studentAnswer; }
    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public String getKnowledgePoint() { return knowledgePoint; }
    public void setKnowledgePoint(String knowledgePoint) { this.knowledgePoint = knowledgePoint; }
    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public int getReviewLevel() { return reviewLevel; }
    public void setReviewLevel(int reviewLevel) { this.reviewLevel = reviewLevel; }
    public boolean isMastered() { return mastered; }
    public void setMastered(boolean mastered) { this.mastered = mastered; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(String reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getNextReviewAt() { return nextReviewAt; }
    public void setNextReviewAt(String nextReviewAt) { this.nextReviewAt = nextReviewAt; }
}
