package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.SerializedName;

public class NoteFragment {
    @SerializedName("milvusId")
    private String milvusId;
    @SerializedName("knowledge_point")
    private String knowledgePoint;
    @SerializedName("content")
    private String content;
    @SerializedName("similarity")
    private double similarity;

    public String getKnowledgePoint() { return knowledgePoint; }
    public void setKnowledgePoint(String knowledgePoint) { this.knowledgePoint = knowledgePoint; }
    public String getMilvusId() { return milvusId; }
    public void setMilvusId(String milvusId) { this.milvusId = milvusId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public double getSimilarity() { return similarity; }
    public void setSimilarity(double similarity) { this.similarity = similarity; }
}
