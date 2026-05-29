package com.suiyuan.iragent.service;

import com.suiyuan.iragent.dto.response.GradingReportResponse;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface GradingPipelineService {

    GradingReportResponse grade(String content, String subjectType, int maxScore,
                                 long userId, Consumer<Map<String, Object>> onStep);

    String diagnoseWrongQuestions(List<Map<String, Object>> questions,
                                   long userId, String subject,
                                   Consumer<Map<String, Object>> onStep);

    void saveReport(String reportId, long userId, String subject, int maxScore, int totalScore,
                    int correctCount, int wrongCount, double accuracy,
                    List<Map<String, Object>> questions);
}
