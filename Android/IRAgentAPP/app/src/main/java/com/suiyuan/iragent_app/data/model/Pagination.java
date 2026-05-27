package com.suiyuan.iragent_app.data.model;

import com.google.gson.annotations.SerializedName;

public class Pagination {
    @SerializedName("total")
    private long total;
    @SerializedName("page")
    private int page;
    @SerializedName("size")
    private int size;
    @SerializedName("totalPages")
    private long totalPages;
    @SerializedName("hasNext")
    private boolean hasNext;
    @SerializedName("hasPrevious")
    private boolean hasPrevious;

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public long getTotalPages() { return totalPages; }
    public void setTotalPages(long totalPages) { this.totalPages = totalPages; }
    public boolean isHasNext() { return hasNext; }
    public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }
    public boolean isHasPrevious() { return hasPrevious; }
    public void setHasPrevious(boolean hasPrevious) { this.hasPrevious = hasPrevious; }
}
