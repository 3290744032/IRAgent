package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DiagnosisItem {
    @SerializedName("title")
    private String title;
    @SerializedName("icon")
    private String icon;
    @SerializedName("analysis")
    private String analysis;
    @SerializedName("points")
    private List<String> points;
    @SerializedName("linked_notes")
    private List<String> linkedNotes;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getAnalysis() { return analysis; }
    public void setAnalysis(String analysis) { this.analysis = analysis; }
    public List<String> getPoints() { return points; }
    public void setPoints(List<String> points) { this.points = points; }
    public List<String> getLinkedNotes() { return linkedNotes; }
    public void setLinkedNotes(List<String> linkedNotes) { this.linkedNotes = linkedNotes; }
}
