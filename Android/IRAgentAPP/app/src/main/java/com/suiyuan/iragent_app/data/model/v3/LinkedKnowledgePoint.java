package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.SerializedName;

public class LinkedKnowledgePoint {
    @SerializedName("id")
    private String id;
    @SerializedName("name")
    private String name;
    @SerializedName("similarity")
    private double similarity;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getSimilarity() { return similarity; }
    public void setSimilarity(double similarity) { this.similarity = similarity; }
}
