package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class NoteDetail {
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
    @SerializedName("content")
    private String content;
    @SerializedName("chunks")
    private List<NoteChunk> chunks;
    @SerializedName("linked_knowledge_points")
    private List<LinkedKnowledgePoint> linkedKnowledgePoints;
    @SerializedName("linked_questions")
    private List<LinkedQuestion> linkedQuestions;
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
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<NoteChunk> getChunks() { return chunks; }
    public void setChunks(List<NoteChunk> chunks) { this.chunks = chunks; }
    public List<LinkedKnowledgePoint> getLinkedKnowledgePoints() { return linkedKnowledgePoints; }
    public void setLinkedKnowledgePoints(List<LinkedKnowledgePoint> linkedKnowledgePoints) { this.linkedKnowledgePoints = linkedKnowledgePoints; }
    public List<LinkedQuestion> getLinkedQuestions() { return linkedQuestions; }
    public void setLinkedQuestions(List<LinkedQuestion> linkedQuestions) { this.linkedQuestions = linkedQuestions; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
