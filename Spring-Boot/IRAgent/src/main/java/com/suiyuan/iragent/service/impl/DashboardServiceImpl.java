package com.suiyuan.iragent.service.impl;

import com.suiyuan.iragent.service.DashboardService;
import com.suiyuan.iragent.service.ErrorBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final JdbcTemplate jdbcTemplate;
    private final ErrorBookService errorBookService;

    @Override
    public Map<String, Object> getOverview(long userId) {
        Integer totalErrors = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM error_book WHERE user_id = ?", Integer.class, userId);
        Integer masteredCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM error_book WHERE user_id = ? AND mastered = true", Integer.class, userId);
        Integer pendingReview = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM error_book WHERE user_id = ? AND next_review_at <= NOW() AND mastered = false",
                Integer.class, userId);
        Integer dueToday = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM error_book WHERE user_id = ? AND next_review_at <= NOW() + INTERVAL '1 day' AND mastered = false",
                Integer.class, userId);

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalErrors", totalErrors != null ? totalErrors : 0);
        overview.put("masteredCount", masteredCount != null ? masteredCount : 0);
        overview.put("pendingReview", pendingReview != null ? pendingReview : 0);
        overview.put("dueToday", dueToday != null ? dueToday : 0);
        int total = totalErrors != null ? totalErrors : 0;
        overview.put("masteryRate", total > 0 ? (double) (masteredCount != null ? masteredCount : 0) / total : 0.0);
        return overview;
    }

    @Override
    public Map<String, Object> getWeeklyReport(long userId) {
        List<Map<String, Object>> dailyStats = jdbcTemplate.queryForList(
                "SELECT DATE(created_at) AS day, COUNT(*) AS count " +
                "FROM error_book WHERE user_id = ? AND created_at >= NOW() - INTERVAL '7 days' " +
                "GROUP BY DATE(created_at) ORDER BY day", userId);

        int weeklyNew = dailyStats.stream().mapToInt(r -> ((Number) r.get("count")).intValue()).sum();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("dailyStats", dailyStats);
        report.put("weeklyNew", weeklyNew);
        return report;
    }

    @Override
    public Map<String, Object> getMasteryRadar(long userId) {
        List<Map<String, Object>> subjects = jdbcTemplate.queryForList(
                "SELECT subject, COUNT(*) AS total, " +
                "SUM(CASE WHEN mastered THEN 1 ELSE 0 END) AS mastered " +
                "FROM error_book WHERE user_id = ? GROUP BY subject", userId);

        List<Map<String, Object>> radarData = new ArrayList<>();
        for (Map<String, Object> row : subjects) {
            int total = ((Number) row.get("total")).intValue();
            int mastered = ((Number) row.get("mastered")).intValue();
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("subject", row.get("subject"));
            point.put("masteryRate", total > 0 ? (double) mastered / total : 0.0);
            radarData.add(point);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("subjects", radarData);
        return result;
    }

    @Override
    public List<Map<String, Object>> getTodayTasks(long userId) {
        return jdbcTemplate.queryForList(
                "SELECT id, question_text AS \"questionText\", knowledge_point AS \"knowledgePoint\", " +
                "subject, review_level AS \"reviewLevel\" " +
                "FROM error_book WHERE user_id = ? AND next_review_at <= NOW() + INTERVAL '1 day' " +
                "AND mastered = false ORDER BY next_review_at ASC LIMIT 10", userId);
    }
}
