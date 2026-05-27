package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GradedQuestion {
    @SerializedName("id")
    private String id;
    @SerializedName("index")
    private int index;
    @SerializedName("questionText")
    private String questionText;
    @SerializedName("studentAnswer")
    private String studentAnswer;
    @SerializedName("correctAnswer")
    private String correctAnswer;
    @SerializedName("isCorrect")
    private boolean isCorrect;
    @SerializedName("score")
    private int score;
    @SerializedName("maxScore")
    private int maxScore;
    @SerializedName("knowledgePoint")
    private String knowledgePoint;
    @SerializedName("diagnosis")
    private DiagnosisJson diagnosis;
    @SerializedName("similarQuestions")
    private List<SimilarQuestion> similarQuestions;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getStudentAnswer() { return studentAnswer; }
    public void setStudentAnswer(String studentAnswer) { this.studentAnswer = studentAnswer; }
    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean correct) { isCorrect = correct; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getMaxScore() { return maxScore; }
    public void setMaxScore(int maxScore) { this.maxScore = maxScore; }
    public String getKnowledgePoint() { return knowledgePoint; }
    public void setKnowledgePoint(String knowledgePoint) { this.knowledgePoint = knowledgePoint; }
    public DiagnosisJson getDiagnosis() { return diagnosis; }
    public void setDiagnosis(DiagnosisJson diagnosis) { this.diagnosis = diagnosis; }
    public List<SimilarQuestion> getSimilarQuestions() { return similarQuestions; }
    public void setSimilarQuestions(List<SimilarQuestion> similarQuestions) { this.similarQuestions = similarQuestions; }
}
