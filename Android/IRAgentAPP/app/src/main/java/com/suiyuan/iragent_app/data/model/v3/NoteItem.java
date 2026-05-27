package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.SerializedName;

public class NoteItem {
    @SerializedName("id")
    private String id;
    @SerializedName("subject")
    private String subject;
    @SerializedName("chapter")
    private String chapter;
    @SerializedName("title")
    private String title;
    @SerializedName("tags")
    private String tags;
    @SerializedName("chunk_count")
    private int chunkCount;
    @SerializedName("linked_question_count")
    private int linkedQuestionCount;
    @SerializedName("created_at")
    private String createdAt;
    @SerializedName("updated_at")
    private String updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getChapter() { return chapter; }
    public void setChapter(String chapter) { this.chapter = chapter; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public int getChunkCount() { return chunkCount; }
    public void setChunkCount(int chunkCount) { this.chunkCount = chunkCount; }
    public int getLinkedQuestionCount() { return linkedQuestionCount; }
    public void setLinkedQuestionCount(int linkedQuestionCount) { this.linkedQuestionCount = linkedQuestionCount; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
