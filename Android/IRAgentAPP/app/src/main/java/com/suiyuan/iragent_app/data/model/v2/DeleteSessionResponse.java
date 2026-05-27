package com.suiyuan.iragent_app.data.model.v2;

import com.google.gson.annotations.SerializedName;

public class DeleteSessionResponse {
    @SerializedName("sessionId")
    private String sessionId;
    @SerializedName("deletedSteps")
    private int deletedSteps;

    public String getSessionId() { return sessionId; }
    public int getDeletedSteps() { return deletedSteps; }
}
