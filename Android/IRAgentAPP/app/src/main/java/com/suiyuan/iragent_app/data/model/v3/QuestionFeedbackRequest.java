package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.SerializedName;

public class QuestionFeedbackRequest {
    @SerializedName("questionId")
    private String questionId;
    @SerializedName("feedbackType")
    private String feedbackType;
    @SerializedName("comment")
    private String comment;

    public QuestionFeedbackRequest(String questionId, String feedbackType, String comment) {
        this.questionId = questionId;
        this.feedbackType = feedbackType;
        this.comment = comment;
    }
}
