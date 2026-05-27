package com.suiyuan.iragent_app.data.model.v2;

import com.google.gson.annotations.SerializedName;

public class HintResponse {
    @SerializedName("hint")
    private String hint;
    @SerializedName("hintsUsed")
    private int hintsUsed;
    @SerializedName("maxHints")
    private int maxHints;

    public String getHint() { return hint; }
    public int getHintsUsed() { return hintsUsed; }
    public int getMaxHints() { return maxHints; }
}
