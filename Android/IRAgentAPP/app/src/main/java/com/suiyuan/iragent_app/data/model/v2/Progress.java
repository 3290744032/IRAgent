package com.suiyuan.iragent_app.data.model.v2;

import com.google.gson.annotations.SerializedName;

public class Progress {
    @SerializedName("currentStep")
    private int currentStep;
    @SerializedName("totalSteps")
    private int totalSteps;
    @SerializedName("masteredSteps")
    private int masteredSteps;
    @SerializedName("remainingSteps")
    private int remainingSteps;
    @SerializedName("skippedSteps")
    private int skippedSteps;

    public int getCurrentStep() { return currentStep; }
    public int getTotalSteps() { return totalSteps; }
    public int getMasteredSteps() { return masteredSteps; }
    public int getRemainingSteps() { return remainingSteps; }
    public int getSkippedSteps() { return skippedSteps; }
}
