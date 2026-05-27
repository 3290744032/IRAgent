package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import java.util.List;

@JsonAdapter(ErrorDetailDeserializer.class)
public class ErrorDetail {
    @SerializedName("id")
    private String id;
    @SerializedName("question_text")
    private String questionText;
    @SerializedName("student_answer")
    private String studentAnswer;
    @SerializedName("correct_answer")
    private String correctAnswer;
    @SerializedName("knowledge_point")
    private String knowledgePoint;
    @SerializedName("error_type")
    private String errorType;
    @SerializedName("subject")
    private String subject;
    @SerializedName("mastered")
    private boolean mastered;
    @SerializedName("diagnosis_json")
    private DiagnosisJson diagnosis;
    @SerializedName("similar_questions")
    private List<SimilarQuestion> similarQuestions;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getStudentAnswer() { return studentAnswer; }
    public void setStudentAnswer(String studentAnswer) { this.studentAnswer = studentAnswer; }
    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public String getKnowledgePoint() { return knowledgePoint; }
    public void setKnowledgePoint(String knowledgePoint) { this.knowledgePoint = knowledgePoint; }
    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public boolean isMastered() { return mastered; }
    public void setMastered(boolean mastered) { this.mastered = mastered; }
    public DiagnosisJson getDiagnosis() { return diagnosis; }
    public void setDiagnosis(DiagnosisJson diagnosis) { this.diagnosis = diagnosis; }
    public List<SimilarQuestion> getSimilarQuestions() { return similarQuestions; }
    public void setSimilarQuestions(List<SimilarQuestion> similarQuestions) { this.similarQuestions = similarQuestions; }
}
