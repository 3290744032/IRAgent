package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DashboardOverview {
    @SerializedName("total_notes")
    private int totalNotes;
    @SerializedName("total_errors")
    private int totalErrors;
    @SerializedName("mastered_errors")
    private int masteredErrors;
    @SerializedName("error_rate")
    private double errorRate;
    @SerializedName("subject_stats")
    private List<SubjectStat> subjectStats;

    public int getTotalNotes() { return totalNotes; }
    public void setTotalNotes(int totalNotes) { this.totalNotes = totalNotes; }
    public int getTotalErrors() { return totalErrors; }
    public void setTotalErrors(int totalErrors) { this.totalErrors = totalErrors; }
    public int getMasteredErrors() { return masteredErrors; }
    public void setMasteredErrors(int masteredErrors) { this.masteredErrors = masteredErrors; }
    public double getErrorRate() { return errorRate; }
    public void setErrorRate(double errorRate) { this.errorRate = errorRate; }
    public List<SubjectStat> getSubjectStats() { return subjectStats; }
    public void setSubjectStats(List<SubjectStat> subjectStats) { this.subjectStats = subjectStats; }

    public static class SubjectStat {
        @SerializedName("subject")
        private String subject;
        @SerializedName("note_count")
        private int noteCount;
        @SerializedName("error_count")
        private int errorCount;

        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public int getNoteCount() { return noteCount; }
        public void setNoteCount(int noteCount) { this.noteCount = noteCount; }
        public int getErrorCount() { return errorCount; }
        public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
    }
}
