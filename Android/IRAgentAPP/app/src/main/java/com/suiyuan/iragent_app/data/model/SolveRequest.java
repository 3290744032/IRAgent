package com.suiyuan.iragent_app.data.model;

import com.google.gson.annotations.SerializedName;

public class SolveRequest {
    @SerializedName("problem")
    private String problem;
    @SerializedName("conversationId")
    private String conversationId;

    public SolveRequest(String problem, String conversationId) {
        this.problem = problem;
        this.conversationId = conversationId;
    }
}
