package com.suiyuan.iragent_app.data.model.v2;

import com.google.gson.annotations.SerializedName;

public class KnowledgeEdge {
    @SerializedName("from")
    private int from;
    @SerializedName("to")
    private int to;
    @SerializedName("label")
    private String label;

    public int getFrom() { return from; }
    public int getTo() { return to; }
    public String getLabel() { return label; }
}
