package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.SerializedName;

public class SearchRequest {
    @SerializedName("query")
    private String query;
    @SerializedName("top_k")
    private int topK;

    public SearchRequest(String query, int topK) {
        this.query = query;
        this.topK = topK;
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }
}
