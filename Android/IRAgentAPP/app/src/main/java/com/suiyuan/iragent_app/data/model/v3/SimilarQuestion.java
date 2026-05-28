package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SimilarQuestion {
    @SerializedName("id")
    private String id;
    @SerializedName("text")
    private String text;
    @SerializedName("tags")
    private List<String> tags;
    @SerializedName("score")
    private Double score;
    @SerializedName("similarity")
    private Double similarity;
    @SerializedName("difficulty")
    private String difficulty;
    @SerializedName("question_type")
    private String questionType;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public Double getSimilarity() { return similarity != null ? similarity : score; }
    public void setSimilarity(Double similarity) { this.similarity = similarity; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
}
