package com.suiyuan.iragent_app.data.model.v2;

import com.google.gson.annotations.SerializedName;

public class CreateSessionRequest {
    @SerializedName("question")
    private String question;
    @SerializedName("subjectType")
    private String subjectType;

    public CreateSessionRequest(String question, String subjectType) {
        this.question = question;
        this.subjectType = subjectType;
    }
}
