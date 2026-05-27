package com.suiyuan.iragent_app.data.model.v2;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SessionHistoryResponse {
    @SerializedName("total")
    private int total;
    @SerializedName("page")
    private int page;
    @SerializedName("size")
    private int size;
    @SerializedName("sessions")
    private List<SessionHistoryItem> sessions;

    public int getTotal() { return total; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public List<SessionHistoryItem> getSessions() { return sessions; }

    public int getTotalPages() {
        if (size <= 0) return 0;
        return (total + size - 1) / size;
    }

    public boolean hasNextPage() {
        return page < getTotalPages();
    }

    public boolean hasPreviousPage() {
        return page > 1;
    }
}
