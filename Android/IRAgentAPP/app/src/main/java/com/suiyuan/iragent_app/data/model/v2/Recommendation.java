package com.suiyuan.iragent_app.data.model.v2;

import com.google.gson.annotations.SerializedName;

public class Recommendation {
    @SerializedName("title")
    private String title;
    @SerializedName("description")
    private String description;

    public String getTitle() { return title; }
    public String getDescription() { return description; }
}
