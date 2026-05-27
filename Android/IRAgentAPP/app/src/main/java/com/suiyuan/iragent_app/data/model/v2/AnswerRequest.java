package com.suiyuan.iragent_app.data.model.v2;

import com.google.gson.annotations.SerializedName;

public class AnswerRequest {
    @SerializedName("answer")
    private String answer;

    public AnswerRequest(String answer) {
        this.answer = answer;
    }
}
