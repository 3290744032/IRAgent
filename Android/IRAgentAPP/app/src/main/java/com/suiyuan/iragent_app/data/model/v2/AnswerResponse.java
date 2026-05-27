package com.suiyuan.iragent_app.data.model.v2;

import com.google.gson.annotations.SerializedName;

public class AnswerResponse {
    @SerializedName("feedbackToken")
    private String feedbackToken;
    @SerializedName("feedbackUrl")
    private String feedbackUrl;

    public String getFeedbackToken() { return feedbackToken; }
    public String getFeedbackUrl() { return feedbackUrl; }
}
