package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.SerializedName;

public class NoteRef {
    @SerializedName("note_fragment")
    private String noteFragment;
    @SerializedName("similarity")
    private double similarity;

    public String getNoteFragment() { return noteFragment; }
    public void setNoteFragment(String noteFragment) { this.noteFragment = noteFragment; }
    public double getSimilarity() { return similarity; }
    public void setSimilarity(double similarity) { this.similarity = similarity; }
}
