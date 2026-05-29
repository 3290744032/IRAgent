package com.suiyuan.iragent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suiyuan.iragent.service.AIQuestionGeneratorService;
import com.suiyuan.iragent.service.DailyPracticeService;
import com.suiyuan.iragent.service.ErrorBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DailyPracticeServiceImpl implements DailyPracticeService {

    private final JdbcTemplate jdbcTemplate;
    private final ErrorBookService errorBookService;
    private final AIQuestionGeneratorService aiGenerator;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> generatePractice(long userId, String subject, int count, String knowledgePoints) {
        List<String> kps = new ArrayList<>();
        if (knowledgePoints != null && !knowledgePoints.isBlank()) {
            Collections.addAll(kps, knowledgePoints.split(","));
        } else {
            List<Map<String, Object>> weakKps = jdbcTemplate.queryForList(
                    "SELECT knowledge_point FROM error_book WHERE user_id = ? AND mastered = false " +
                    "GROUP BY knowledge_point ORDER BY COUNT(*) DESC", userId);
            for (Map<String, Object> row : weakKps) {
                kps.add(row.get("knowledge_point").toString());
            }
            if (kps.isEmpty()) kps.add(subject != null ? subject : "数学");
        }

        List<Map<String, Object>> questions = new ArrayList<>();
        int perKp = Math.max(1, count / kps.size());
        for (String kp : kps) {
            if (questions.size() >= count) break;
            Map<String, Object> q = aiGenerator.generateOne(
                    new AIQuestionGeneratorService.QuestionContext(
                            subject, null, kp, "single_choice", 3, null, null, null));
            if (q != null) questions.add(q);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("practiceId", "dp-" + UUID.randomUUID().toString().substring(0, 12));
        result.put("subject", subject);
        result.put("questionCount", questions.size());
        result.put("questions", questions);
        return result;
    }

    @Override
    public Map<String, Object> submitAnswers(long userId, Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> answers = (List<Map<String, Object>>) body.get("answers");
        int correctCount = 0;
        int totalCount = answers != null ? answers.size() : 0;

        if (answers != null) {
            for (Map<String, Object> answer : answers) {
                String questionId = (String) answer.get("questionId");
                String studentAnswer = (String) answer.get("answer");
                String correctAnswer = (String) answer.get("correctAnswer");
                boolean isCorrect = studentAnswer != null && studentAnswer.equalsIgnoreCase(correctAnswer);
                if (isCorrect) correctCount++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCount", totalCount);
        result.put("correctCount", correctCount);
        result.put("score", totalCount > 0 ? correctCount * 100 / totalCount : 0);
        return result;
    }
}
