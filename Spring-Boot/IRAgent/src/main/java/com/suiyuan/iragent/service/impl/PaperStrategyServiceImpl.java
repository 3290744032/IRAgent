package com.suiyuan.iragent.service.impl;

import com.suiyuan.iragent.service.PaperStrategyService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class PaperStrategyServiceImpl implements PaperStrategyService {

    private final JdbcTemplate jdbcTemplate;

    public PaperConfig computeConfig(long userId, String subject, List<String> givenKps, int questionCount) {
        List<String> kps = (givenKps != null && !givenKps.isEmpty()) ? givenKps : getWeakKps(userId, 4);

        double avgMastery = getAvgMastery(userId);
        DifficultySplit split = computeDifficultySplit(avgMastery);

        return new PaperConfig(kps, split, new int[]{40, 30, 30});
    }

    @SuppressWarnings("unchecked")
    private List<String> getWeakKps(long userId, int maxCount) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT knowledge_point FROM error_book WHERE user_id = ? AND mastered = false " +
                    "GROUP BY knowledge_point ORDER BY COUNT(*) DESC LIMIT ?", userId, maxCount);
            List<String> kps = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                Object kp = row.get("knowledge_point");
                if (kp != null) kps.add(kp.toString());
            }
            return kps.isEmpty() ? List.of() : kps;
        } catch (Exception e) { return List.of(); }
    }

    private double getAvgMastery(long userId) {
        try {
            Integer mastered = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM error_book WHERE user_id = ? AND mastered = true", Integer.class, userId);
            Integer total = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM error_book WHERE user_id = ?", Integer.class, userId);
            if (total == null || total == 0) return 0.5;
            return (double) (mastered != null ? mastered : 0) / total;
        } catch (Exception e) { return 0.5; }
    }

    private DifficultySplit computeDifficultySplit(double avgMastery) {
        if (avgMastery < 0.4) return new DifficultySplit(60, 30, 10);
        if (avgMastery < 0.7) return new DifficultySplit(20, 50, 30);
        return new DifficultySplit(10, 40, 50);
    }

    public int[] getTypeRatios() {
        return new int[]{40, 30, 30};
    }
}
