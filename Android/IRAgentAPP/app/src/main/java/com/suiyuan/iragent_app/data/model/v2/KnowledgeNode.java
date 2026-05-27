package com.suiyuan.iragent_app.data.model.v2;

import com.google.gson.annotations.SerializedName;

public class KnowledgeNode {
    @SerializedName("id")
    private int id;
    @SerializedName("name")
    private String name;
    @SerializedName("status")
    private String status;
    @SerializedName("proficiency")
    private int proficiency;

    public int getId() { return id; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public int getProficiency() { return proficiency; }
}
