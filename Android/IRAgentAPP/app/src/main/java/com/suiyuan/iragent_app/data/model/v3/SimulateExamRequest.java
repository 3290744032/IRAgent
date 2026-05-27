package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.SerializedName;

public class SimulateExamRequest {
    @SerializedName("subject")
    private String subject;
    @SerializedName("examType")
    private String examType;
    @SerializedName("count")
    private int count;

    public SimulateExamRequest(String subject, String examType, int count) {
        this.subject = subject;
        this.examType = examType;
        this.count = count;
    }
}
