package com.suiyuan.iragent.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suiyuan.iragent.service.ErrorBookService;
import com.suiyuan.iragent.service.SpacedRepetitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorBookServiceImpl implements ErrorBookService {

    private final JdbcTemplate jdbcTemplate;
    private final SpacedRepetitionService spacedRepetition;
    private final ObjectMapper objectMapper;

    @Transactional
    @Override
    public void addFromGrading(long userId, String reportId, String questionText,
                                String studentAnswer, String correctAnswer,
                                String knowledgePoint, String subject, String errorType,
                                Map<String, Object> diagnosis, List<Map<String, Object>> similar) {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        try {
            jdbcTemplate.update(
                    "INSERT INTO error_book (id, user_id, question_text, student_answer, correct_answer, " +
                    "knowledge_point, subject, error_type, diagnosis_json, similar_questions, " +
                    "review_level, next_review_at, source_report_id) " +
                    "VALUES (?,?,?,?,?,?,?,?,?::jsonb,?::jsonb,0,NOW(),?)",
                    id, userId, questionText, studentAnswer, correctAnswer,
                    knowledgePoint, subject, errorType,
                    diagnosis != null ? objectMapper.writeValueAsString(diagnosis) : null,
                    similar != null ? objectMapper.writeValueAsString(similar) : null,
                    reportId);
        } catch (Exception e) {
            log.error("错题入库失败", e);
        }
    }

    @Override
    public List<Map<String, Object>> listErrors(long userId, String subject,
                                                  String errorType, int page, int size) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, question_text, student_answer, correct_answer, knowledge_point, " +
                "subject, error_type, review_level, next_review_at, mastered, created_at " +
                "FROM error_book WHERE user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (subject != null && !subject.isBlank()) {
            sql.append(" AND subject = ?");
            params.add(subject);
        }
        if (errorType != null && !errorType.isBlank()) {
            sql.append(" AND error_type = ?");
            params.add(errorType);
        }
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(page * size);

        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    @Override
    public Map<String, Object> getErrorDetail(long userId, String id) {
        return jdbcTemplate.queryForMap(
                "SELECT * FROM error_book WHERE id = ? AND user_id = ?", id, userId);
    }

    @Override
    public List<Map<String, Object>> getReviewQueue(long userId) {
        return jdbcTemplate.queryForList(
                "SELECT id, question_text, knowledge_point, subject, review_level " +
                "FROM error_book WHERE user_id = ? AND next_review_at <= NOW() AND mastered = false " +
                "ORDER BY next_review_at ASC LIMIT 20",
                userId);
    }

    @Transactional
    @Override
    public void markMastered(long userId, String id) {
        jdbcTemplate.update(
                "UPDATE error_book SET mastered = true, review_level = ? WHERE id = ? AND user_id = ?",
                spacedRepetition.getMaxLevel(), id, userId);
    }

    @Transactional
    @Override
    public void unmarkMastered(long userId, String id) {
        jdbcTemplate.update(
                "UPDATE error_book SET mastered = false, review_level = 0 WHERE id = ? AND user_id = ?",
                id, userId);
    }

    @Override
    public List<Map<String, Object>> getSimilarQuestions(long userId, String id) {
        var error = jdbcTemplate.queryForMap(
                "SELECT similar_questions FROM error_book WHERE id = ? AND user_id = ?", id, userId);
        Object similar = error.get("similar_questions");
        if (similar == null) return List.of();
        try {
            return objectMapper.readValue(similar.toString(),
                    new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
