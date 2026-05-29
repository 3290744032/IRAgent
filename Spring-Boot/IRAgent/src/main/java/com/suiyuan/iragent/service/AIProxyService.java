package com.suiyuan.iragent.service;

import java.util.Map;

public interface AIProxyService {
    
    Map<String, Object> generateTeachingContent(String question, String subjectType);
    
    String generateAnswer(String userQuestion, String context, String teachingContent);
    
    Map<String, Object> generateSummary(String question, int completedSteps, String totalTime);
}
