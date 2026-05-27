package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.SerializedName;

public class TaskItem {
    @SerializedName("type")
    private String type;
    @SerializedName("title")
    private String title;
    @SerializedName("count")
    private int count;
    @SerializedName("description")
    private String description;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
