package com.suiyuan.iragent_app.data.model.v2;

import com.google.gson.annotations.SerializedName;

public class MasterResponse {
    @SerializedName("status")
    private String status;
    @SerializedName("nextStep")
    private NextStep nextStep;
    @SerializedName("isCompleted")
    private boolean isCompleted;

    public String getStatus() { return status; }
    public NextStep getNextStep() { return nextStep; }
    public boolean isCompleted() { return isCompleted; }

    public static class NextStep {
        @SerializedName("index")
        private int index;
        @SerializedName("title")
        private String title;

        public int getIndex() { return index; }
        public String getTitle() { return title; }
    }
}
