package com.suiyuan.iragent_app.data.model.v2;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MasterySummary {
    @SerializedName("已掌握")
    private List<String> masteredPoints;
    @SerializedName("需要加强")
    private List<String> weakPoints;

    public List<String> getMasteredPoints() { return masteredPoints; }
    public List<String> getWeakPoints() { return weakPoints; }
}
