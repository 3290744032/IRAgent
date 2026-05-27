package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import java.util.List;

@JsonAdapter(PracticeQuestionDeserializer.class)
public class PracticeQuestion {
    private String id;
    private String questionText;
    private String questionType;
    private List<String> options;
    private int difficulty;
    private String knowledgePoint;
    private String chapter;
    private int score;
    private int index;
    private String source;

    public String getId() { return id; }
    public String getQuestionText() { return questionText; }
    public String getQuestionType() { return questionType; }
    public List<String> getOptions() { return options; }
    public int getDifficulty() { return difficulty; }
    public String getKnowledgePoint() { return knowledgePoint; }
    public String getChapter() { return chapter; }
    public int getScore() { return score; }
    public int getIndex() { return index; }
    public String getSource() { return source; }

    void setId(String id) { this.id = id; }
    void setQuestionText(String questionText) { this.questionText = questionText; }
    void setQuestionType(String questionType) { this.questionType = questionType; }
    void setOptions(List<String> options) { this.options = options; }
    void setDifficulty(int difficulty) { this.difficulty = difficulty; }
    void setKnowledgePoint(String knowledgePoint) { this.knowledgePoint = knowledgePoint; }
    void setChapter(String chapter) { this.chapter = chapter; }
    void setScore(int score) { this.score = score; }
    void setIndex(int index) { this.index = index; }
    void setSource(String source) { this.source = source; }
}
