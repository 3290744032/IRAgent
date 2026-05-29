package com.suiyuan.iragent.service.impl;

import com.suiyuan.iragent.service.*;
import com.suiyuan.iragent.service.PaperStrategyService.DifficultySplit;
import com.suiyuan.iragent.service.PaperStrategyService.PaperConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmartPaperServiceImpl implements SmartPaperService {

    private final JdbcTemplate jdbcTemplate;
    private final PaperStrategyService strategyService;
    private final DailyPracticeService dailyPracticeService;
    private final AIQuestionGeneratorService aiGenerator;

    public Map<String, Object> generatePaper(long userId, String subject, String examType,
                                              String title, int questionCount, int difficulty,
                                              List<String> kps, boolean excludeDone) {
        PaperConfig config = strategyService.computeConfig(userId, subject, kps, questionCount);
        DifficultySplit split = config.split();
        int[] typeRatios = config.typeRatios();

        int easyCount = questionCount * split.easy() / 100;
        int mediumCount = questionCount * split.medium() / 100;
        int hardCount = questionCount - easyCount - mediumCount;

        List<Map<String, Object>> questions = new ArrayList<>();
        String[] types = {"single_choice", "fill_blank", "calculation"};
        int[] typeCounts = {
                questionCount * typeRatios[0] / 100,
                questionCount * typeRatios[1] / 100,
                questionCount - (questionCount * typeRatios[0] / 100) - (questionCount * typeRatios[1] / 100)
        };

        for (String kp : config.knowledgePoints()) {
            for (int t = 0; t < types.length; t++) {
                if (typeCounts[t] <= 0) continue;
                int[] diffs = {2, 3, 4, 1, 5};
                for (int d : diffs) {
                    if (typeCounts[t] <= 0) break;
                    Map<String, Object> aiQ = aiGenerator.generateOne(
                            new AIQuestionGeneratorService.QuestionContext(
                                    subject, null, kp, types[t], d, examType, null, null));
                    if (aiQ != null) {
                        questions.add(aiQ);
                        typeCounts[t]--;
                    } else {
                        List<Map<String, Object>> found = fetchQuestions(
                                subject, kp, types[t], d, 1, excludeDone, userId);
                        if (!found.isEmpty()) { questions.addAll(found); typeCounts[t] -= found.size(); }
                    }
                    if (questions.size() >= questionCount) break;
                }
                if (questions.size() >= questionCount) break;
            }
            if (questions.size() >= questionCount) break;
        }

        int totalScore = 100;
        int scorePerQuestion = totalScore / Math.max(questions.size(), 1);

        String paperId = "sp-" + UUID.randomUUID().toString().substring(0, 12);
        for (int i = 0; i < questions.size(); i++) {
            questions.get(i).put("index", i + 1);
            questions.get(i).put("score", scorePerQuestion);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("paperId", paperId);
        result.put("title", title);
        result.put("subject", subject);
        result.put("examType", examType);
        result.put("totalScore", totalScore);
        result.put("questionCount", questions.size());
        result.put("estimatedTime", questions.size() * 5);
        result.put("questions", questions);
        return result;
    }

    private List<Map<String, Object>> fetchQuestions(String subject, String kp, String type,
                                                      int difficulty, int limit, boolean excludeDone, long userId) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, question_text AS \"questionText\", question_type AS \"questionType\", " +
                "options, difficulty, knowledge_point AS \"knowledgePoint\", chapter FROM question " +
                "WHERE status = 'published' AND subject = ? AND knowledge_point = ? " +
                "AND question_type = ? AND difficulty = ?");
        List<Object> params = new ArrayList<>(List.of(subject, kp, type, difficulty));

        if (excludeDone) {
            sql.append(" AND id NOT IN (SELECT question_id FROM user_answer_record WHERE user_id = ? AND is_correct = true)");
            params.add(userId);
        }
        sql.append(" ORDER BY RANDOM() LIMIT ?");
        params.add(limit);

        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public Map<String, Object> submitAnswers(long userId, Map<String, Object> body) {
        return dailyPracticeService.submitAnswers(userId, body);
    }
}
