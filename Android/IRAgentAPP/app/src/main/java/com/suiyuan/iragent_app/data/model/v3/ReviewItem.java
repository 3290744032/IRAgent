package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.SerializedName;

public class ReviewItem {
    @SerializedName("error_id")
    private String errorId;
    @SerializedName("question_text")
    private String questionText;
    @SerializedName("knowledge_point")
    private String knowledgePoint;
    @SerializedName("subject")
    private String subject;
    @SerializedName("review_level")
    private int reviewLevel;
    @SerializedName("next_review_at")
    private String nextReviewAt;

    public String getErrorId() { return errorId; }
    public void setErrorId(String errorId) { this.errorId = errorId; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getKnowledgePoint() { return knowledgePoint; }
    public void setKnowledgePoint(String knowledgePoint) { this.knowledgePoint = knowledgePoint; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public int getReviewLevel() { return reviewLevel; }
    public void setReviewLevel(int reviewLevel) { this.reviewLevel = reviewLevel; }
    public String getNextReviewAt() { return nextReviewAt; }
    public void setNextReviewAt(String nextReviewAt) { this.nextReviewAt = nextReviewAt; }
}
