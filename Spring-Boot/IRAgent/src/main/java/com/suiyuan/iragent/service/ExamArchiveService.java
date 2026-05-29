package com.suiyuan.iragent.service;

import java.util.List;
import java.util.Map;

public interface ExamArchiveService {

    List<Map<String, Object>> listQuestions(String subject, Integer year, String examType,
                                             String knowledgePoint, Integer difficulty,
                                             int page, int size);

    List<Map<String, Object>> simulateQuestions(String subject, String examType, int count);

    Map<String, Object> getFilters();

    Map<String, Object> ingestQuestions(String rawResponse, String subject, Long userId);
}
