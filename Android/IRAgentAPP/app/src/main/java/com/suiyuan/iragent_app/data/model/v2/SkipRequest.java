package com.suiyuan.iragent_app.data.model.v2;

import com.google.gson.annotations.SerializedName;

public class SkipRequest {
    @SerializedName("reason")
    private String reason;

    public SkipRequest(String reason) {
        this.reason = reason;
    }
}
