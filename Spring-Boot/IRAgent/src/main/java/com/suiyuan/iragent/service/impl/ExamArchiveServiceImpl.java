package com.suiyuan.iragent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suiyuan.iragent.service.AIQuestionGeneratorService;
import com.suiyuan.iragent.service.ExamArchiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamArchiveServiceImpl implements ExamArchiveService {

    private final JdbcTemplate jdbcTemplate;
    private final AIQuestionGeneratorService aiGenerator;
    private final ObjectMapper objectMapper;

    @Override
    public List<Map<String, Object>> listQuestions(String subject, Integer year, String examType,
                                                    String knowledgePoint, Integer difficulty,
                                                    int page, int size) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, question_text AS \"questionText\", question_type AS \"questionType\", " +
                "options, correct_answer AS \"correctAnswer\", explanation, difficulty, " +
                "subject, chapter, knowledge_point AS \"knowledgePoint\", year, exam_type AS \"examType\", source " +
                "FROM question WHERE status = 'published'");
        List<Object> params = new ArrayList<>();
        appendFilter(sql, params, "subject", subject);
        appendFilter(sql, params, "year", year);
        appendFilter(sql, params, "exam_type", examType);
        appendFilter(sql, params, "knowledge_point", knowledgePoint);
        appendFilter(sql, params, "difficulty", difficulty);
        sql.append(" ORDER BY year DESC, id ASC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(page * size);
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    @Override
    public List<Map<String, Object>> simulateQuestions(String subject, String examType, int count) {
        String[] types = {"single_choice", "single_choice", "single_choice", "fill_blank", "fill_blank", "calculation"};
        List<Map<String, Object>> questions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String type = types[i % types.length];
            int diff = i < count * 3 / 10 ? 2 : (i < count * 7 / 10 ? 3 : 4);
            Map<String, Object> q = aiGenerator.generateOne(
                    new AIQuestionGeneratorService.QuestionContext(subject, null, "综合", type, diff, examType, null, null));
            if (q != null) questions.add(q);
        }
        return questions;
    }

    @Override
    public Map<String, Object> getFilters() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("years", jdbcTemplate.queryForList(
                "SELECT DISTINCT year FROM question WHERE status = 'published' ORDER BY year DESC", Integer.class));
        filters.put("examTypes", jdbcTemplate.queryForList(
                "SELECT DISTINCT exam_type FROM question WHERE status = 'published' AND exam_type IS NOT NULL", String.class));
        filters.put("subjects", jdbcTemplate.queryForList(
                "SELECT DISTINCT subject FROM question WHERE status = 'published'", String.class));
        return filters;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> ingestQuestions(String rawResponse, String subject, Long userId) {
        try {
            String json = rawResponse;
            int start = json.indexOf("[");
            int end = json.lastIndexOf("]");
            if (start >= 0 && end > start) json = json.substring(start, end + 1);
            List<Map<String, Object>> questions = objectMapper.readValue(json, List.class);
            int count = 0;
            for (Map<String, Object> q : questions) {
                String id = "up-" + UUID.randomUUID().toString().substring(0, 12);
                jdbcTemplate.update(
                    "INSERT INTO question (id, question_text, question_type, options, correct_answer, " +
                    "explanation, difficulty, subject, knowledge_point, source, status) " +
                    "VALUES (?,?,?,?::jsonb,?,'',?,?,?,'user-uploaded','published')",
                    id,
                    q.get("questionText"),
                    q.getOrDefault("questionType", "calculation"),
                    objectMapper.writeValueAsString(q.getOrDefault("options", List.of())),
                    q.get("correctAnswer"),
                    q.get("difficulty") instanceof Number n ? n.intValue() : 3,
                    subject,
                    q.get("knowledgePoint"));
                count++;
            }
            return Map.of("ingested", count);
        } catch (Exception e) {
            return Map.of("ingested", 0, "error", e.getMessage());
        }
    }

    private void appendFilter(StringBuilder sql, List<Object> params,
                               String column, Object value) {
        if (value == null) return;
        if (value instanceof String s && s.isBlank()) return;
        if (params.isEmpty() || !sql.toString().contains("WHERE")) {
            sql.append(" WHERE ");
        } else {
            sql.append(" AND ");
        }
        sql.append(column).append(" = ?");
        params.add(value);
    }
}
