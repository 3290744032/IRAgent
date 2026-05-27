package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.SerializedName;

public class UploadResult {
    @SerializedName("noteId")
    private String noteId;
    @SerializedName("chunkCount")
    private int chunkCount;
    @SerializedName("classification")
    private Classification classification;

    public String getNoteId() { return noteId; }
    public int getChunkCount() { return chunkCount; }
    public Classification getClassification() { return classification; }

    public static class Classification {
        @SerializedName("subject") private String subject;
        @SerializedName("chapter") private String chapter;
        @SerializedName("tags") private String tags;
        public String getSubject() { return subject; }
        public String getChapter() { return chapter; }
        public String getTags() { return tags; }
    }
}
