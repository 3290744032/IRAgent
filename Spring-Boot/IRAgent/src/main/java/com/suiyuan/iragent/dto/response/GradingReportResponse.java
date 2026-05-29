package com.suiyuan.iragent.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class GradingReportResponse {
    private String reportId;
    private int totalScore;
    private int maxScore;
    private int correctCount;
    private int wrongCount;
    private double accuracy;
    private List<QuestionDetail> questions;

    @Data
    @Builder
    public static class QuestionDetail {
        private String id;
        private int index;
        private String questionText;
        private String studentAnswer;
        private String correctAnswer;
        private boolean isCorrect;
        private int score;
        private int maxScore;
        private String knowledgePoint;
        private Map<String, Object> diagnosis;
        private List<Map<String, Object>> similarQuestions;
    }
}
