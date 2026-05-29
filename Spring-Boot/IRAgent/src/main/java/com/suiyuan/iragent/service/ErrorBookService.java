package com.suiyuan.iragent.service;

import java.util.List;
import java.util.Map;

public interface ErrorBookService {

    void addFromGrading(long userId, String reportId, String questionText,
                        String studentAnswer, String correctAnswer,
                        String knowledgePoint, String subject, String errorType,
                        Map<String, Object> diagnosis, List<Map<String, Object>> similar);

    List<Map<String, Object>> listErrors(long userId, String subject,
                                          String errorType, int page, int size);

    Map<String, Object> getErrorDetail(long userId, String id);

    List<Map<String, Object>> getReviewQueue(long userId);

    void markMastered(long userId, String id);

    void unmarkMastered(long userId, String id);

    List<Map<String, Object>> getSimilarQuestions(long userId, String id);
}
