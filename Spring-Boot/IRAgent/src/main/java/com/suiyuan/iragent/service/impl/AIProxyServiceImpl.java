package com.suiyuan.iragent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suiyuan.iragent.config.PromptTemplateManager;
import com.suiyuan.iragent.config.VolcEngineChatClient;
import com.suiyuan.iragent.service.AIProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIProxyServiceImpl implements AIProxyService {

    private final VolcEngineChatClient chatClient;
    private final PromptTemplateManager promptManager;
    private final ObjectMapper objectMapper;

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> generateTeachingContent(String question, String subjectType) {
        try {
            Map<String, String> variables = new HashMap<>();
            variables.put("question", question);
            variables.put("subjectType", subjectType != null ? subjectType : "general");
            
            String prompt = promptManager.renderWithMap("TEACH", variables);
            String response = chatClient.chat(prompt);
            
            return parseJsonResponse(response);
        } catch (Exception e) {
            log.error("生成讲解内容失败: question={}, error={}", question, e.getMessage(), e);
            return createErrorResponse("生成讲解内容失败: " + e.getMessage());
        }
    }

    @Override
    public String generateAnswer(String userQuestion, String context, String teachingContent) {
        try {
            Map<String, String> variables = new HashMap<>();
            variables.put("userQuestion", userQuestion);
            variables.put("context", context != null ? context : "");
            variables.put("teachingContent", teachingContent != null ? teachingContent : "");
            
            String prompt = promptManager.renderWithMap("ANSWER", variables);
            return chatClient.chat(prompt);
        } catch (Exception e) {
            log.error("生成回答失败: question={}, error={}", userQuestion, e.getMessage(), e);
            return "抱歉，我无法回答这个问题，请稍后重试。";
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> generateSummary(String question, int completedSteps, String totalTime) {
        try {
            Map<String, String> variables = new HashMap<>();
            variables.put("question", question);
            variables.put("completedSteps", String.valueOf(completedSteps));
            variables.put("totalTime", totalTime != null ? totalTime : "未知");
            
            String prompt = promptManager.renderWithMap("SUMMARY", variables);
            String response = chatClient.chat(prompt);
            
            return parseJsonResponse(response);
        } catch (Exception e) {
            log.error("生成学习总结失败: question={}, error={}", question, e.getMessage(), e);
            return createErrorResponse("生成学习总结失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonResponse(String response) {
        try {
            if (response == null || response.trim().isEmpty()) {
                log.warn("AI返回空响应");
                return createErrorResponse("AI返回空响应");
            }
            
            String cleanJson = extractJsonFromResponse(response);
            return (Map<String, Object>) objectMapper.readValue(cleanJson, Map.class);
        } catch (Exception e) {
            log.error("JSON解析失败: response={}, error={}", response, e.getMessage());
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("_raw", response);
            fallback.put("_parseError", e.getMessage());
            return fallback;
        }
    }

    private String extractJsonFromResponse(String response) {
        String trimmed = response.trim();
        
        int jsonStart = -1;
        int jsonEnd = -1;
        
        for (int i = 0; i < trimmed.length(); i++) {
            if (trimmed.charAt(i) == '{') {
                jsonStart = i;
                break;
            }
        }
        
        if (jsonStart == -1) {
            return trimmed;
        }
        
        int braceCount = 0;
        for (int i = jsonStart; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '{') braceCount++;
            else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    jsonEnd = i + 1;
                    break;
                }
            }
        }
        
        if (jsonEnd > jsonStart) {
            return trimmed.substring(jsonStart, jsonEnd);
        }
        
        return trimmed;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("_error", true);
        error.put("_errorMessage", message);
        return error;
    }
}
