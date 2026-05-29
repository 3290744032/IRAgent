package com.suiyuan.iragent.service;

import com.suiyuan.iragent.dto.response.CreateSessionResponse;
import com.suiyuan.iragent.dto.response.MasterResponse;
import com.suiyuan.iragent.entity.LearningSession;

import java.util.List;
import java.util.Map;

public interface LearningSessionService {
    
    CreateSessionResponse createSession(Long userId, String question, String subjectType);
    
    Map<String, Object> getSessionDetail(String sessionId);
    
    List<LearningSession> getSessionsByUserId(Long userId);
    
    Map<String, Object> getSessionHistory(Long userId, int page, int size);
    
    boolean deleteSession(String sessionId, Long userId);
    
    Map<String, Object> getSessionSummary(String sessionId);

    boolean hasSummaryCache(String sessionId);

    Map<String, Object> saveGeneratedSummary(String sessionId, String aiResponseJson);
    
    MasterResponse markAsMastered(String sessionId, Integer stepIndex);
    
    String getStepTeachingContent(String sessionId, Integer stepIndex);

    String getStepInfoForTeaching(String sessionId, Integer stepIndex);

    String getQuestion(String sessionId);

    Map<String, Object> answerUserQuestion(String sessionId, Integer stepIndex, String userQuestion);

    String buildAnswerPrompt(String sessionId, Integer stepIndex, String userQuestion);

    String buildSummaryPrompt(String sessionId);

    int getCurrentRound(String sessionId);

    void recordAnswer(String sessionId, String answer, String question);

    String buildTeachPrompt(String sessionId, String question);

    void appendTeacherMessage(String sessionId, String content);

    void clearHistory(String sessionId);
}
