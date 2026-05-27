package com.suiyuan.iragent_app.data.model.v2;

import com.google.gson.annotations.SerializedName;

public class SkipResponse {
    @SerializedName("skippedStep")
    private LearningStep skippedStep;
    @SerializedName("isCompleted")
    private boolean isCompleted;
    @SerializedName("progress")
    private Progress progress;

    public LearningStep getSkippedStep() { return skippedStep; }
    public boolean isCompleted() { return isCompleted; }
    public Progress getProgress() { return progress; }
}
