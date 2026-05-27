package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.SerializedName;

public class WeeklyReport {
    @SerializedName("week_errors")
    private int weekErrors;
    @SerializedName("week_reports")
    private int weekReports;
    @SerializedName("week_notes")
    private int weekNotes;
    @SerializedName("total_activity")
    private int totalActivity;

    public int getWeekErrors() { return weekErrors; }
    public void setWeekErrors(int weekErrors) { this.weekErrors = weekErrors; }
    public int getWeekReports() { return weekReports; }
    public void setWeekReports(int weekReports) { this.weekReports = weekReports; }
    public int getWeekNotes() { return weekNotes; }
    public void setWeekNotes(int weekNotes) { this.weekNotes = weekNotes; }
    public int getTotalActivity() { return totalActivity; }
    public void setTotalActivity(int totalActivity) { this.totalActivity = totalActivity; }
}
