package com.suiyuan.iragent_app.data.model.v2;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class KnowledgeGraph {
    @SerializedName("核心知识点")
    private List<String> coreKnowledgePoints;

    public List<String> getCoreKnowledgePoints() { return coreKnowledgePoints; }
}
