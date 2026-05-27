package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ExamQuestion {
    @SerializedName("id") private String id;
    @SerializedName("question_text") private String questionText;
    @SerializedName("question_type") private String questionType;
    @SerializedName("options") private List<String> options;
    @SerializedName("correct_answer") private String correctAnswer;
    @SerializedName("explanation") private String explanation;
    @SerializedName("difficulty") private int difficulty;
    @SerializedName("subject") private String subject;
    @SerializedName("chapter") private String chapter;
    @SerializedName("knowledge_point") private String knowledgePoint;
    @SerializedName("tags") private List<String> tags;
    @SerializedName("year") private int year;
    @SerializedName("exam_type") private String examType;
    @SerializedName("source") private String source;
    @SerializedName("linked_official_id") private String linkedOfficialId;
    @SerializedName("status") private String status;
    @SerializedName("created_at") private String createdAt;
    @SerializedName("updated_at") private String updatedAt;

    public String getId() { return id; }
    public String getQuestionText() { return questionText; }
    public String getQuestionType() { return questionType; }
    public List<String> getOptions() { return options; }
    public String getCorrectAnswer() { return correctAnswer; }
    public String getExplanation() { return explanation; }
    public int getDifficulty() { return difficulty; }
    public String getSubject() { return subject; }
    public String getChapter() { return chapter; }
    public String getKnowledgePoint() { return knowledgePoint; }
    public List<String> getTags() { return tags; }
    public int getYear() { return year; }
    public String getExamType() { return examType; }
    public String getSource() { return source; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
}
