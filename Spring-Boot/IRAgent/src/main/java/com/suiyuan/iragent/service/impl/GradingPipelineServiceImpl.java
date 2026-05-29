package com.suiyuan.iragent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suiyuan.iragent.config.VolcEngineChatClient;
import com.suiyuan.iragent.dto.response.GradingReportResponse;
import com.suiyuan.iragent.dto.response.GradingReportResponse.QuestionDetail;
import com.suiyuan.iragent.rag.retrieval.AdaptiveRecallService;
import com.suiyuan.iragent.service.DiagnosisService;
import com.suiyuan.iragent.service.ErrorBookService;
import com.suiyuan.iragent.service.GradingPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class GradingPipelineServiceImpl implements GradingPipelineService {

    private final VolcEngineChatClient chatClient;
    private final DiagnosisService diagnosisService;
    private final AdaptiveRecallService recallService;
    private final ErrorBookService errorBookService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public GradingReportResponse grade(String content, String subjectType, int maxScore,
                                        long userId, Consumer<Map<String, Object>> onStep) {
        String reportId = UUID.randomUUID().toString().substring(0, 16);

        onStep.accept(Map.of("step", "ocr", "text", content));
        log.info("批改 Step 1/4: OCR 完成, length={}", content.length());

        List<ParsedQuestion> questions = extractQuestions(content);
        onStep.accept(Map.of("step", "extract", "count", questions.size()));
        log.info("批改 Step 2/4: 提取 {} 道题目", questions.size());

        int scorePerQuestion = maxScore / Math.max(questions.size(), 1);
        List<QuestionDetail> details = new ArrayList<>();
        int correctCount = 0, wrongCount = 0, totalScore = 0;

        for (int i = 0; i < questions.size(); i++) {
            ParsedQuestion q = questions.get(i);
            onStep.accept(Map.of("step", "grade", "current", i + 1, "total", questions.size()));

            String gradingPrompt = buildGradingPrompt(q.question, q.studentAnswer, subjectType);
            String gradingResult = chatClient.chat(gradingPrompt);

            boolean isCorrect = isCorrectAnswer(gradingResult);
            if (isCorrect) {
                correctCount++;
                totalScore += scorePerQuestion;
            } else {
                wrongCount++;
            }

            Map<String, Object> diagnosis = null;
            List<Map<String, Object>> similar = null;
            if (!isCorrect) {
                onStep.accept(Map.of("step", "diagnose", "current", i + 1));
                try {
                    var diagResults = diagnosisService.diagnose(
                            q.question, q.studentAnswer, subjectType,
                            String.valueOf(userId),
                            (nodeId, text) -> {},
                            r -> {});
                    diagnosis = new HashMap<>();
                    for (var entry : diagResults.entrySet()) {
                        diagnosis.put(entry.getKey(),
                                entry.getValue().getOutput() != null
                                        ? entry.getValue().getOutput().get("content") : "");
                    }
                } catch (Exception e) {
                    log.error("诊断失败: question={}", i, e);
                }

                try {
                    List<String> tags = extractKnowledgePoints(gradingResult);
                    similar = recallService.recall(q.question, tags, 3).stream()
                            .map(r -> Map.<String, Object>of(
                                    "id", r.id(), "text", r.questionText(),
                                    "tags", r.tags(), "score", r.rrfScore()))
                            .toList();
                } catch (Exception e) {
                    log.error("同类题推荐失败", e);
                }

                try {
                    errorBookService.addFromGrading(userId, reportId,
                            q.question, q.studentAnswer,
                            extractCorrectAnswer(gradingResult),
                            extractKnowledgePoint(gradingResult),
                            subjectType,
                            detectErrorType(gradingResult),
                            diagnosis, similar);
                } catch (Exception e) {
                    log.error("错题入库失败", e);
                }
            }

            QuestionDetail detail = QuestionDetail.builder()
                    .id(reportId + "-q" + (i + 1))
                    .index(i + 1)
                    .questionText(q.question)
                    .studentAnswer(q.studentAnswer)
                    .correctAnswer(extractCorrectAnswer(gradingResult))
                    .isCorrect(isCorrect)
                    .score(isCorrect ? scorePerQuestion : 0)
                    .maxScore(scorePerQuestion)
                    .knowledgePoint(extractKnowledgePoint(gradingResult))
                    .diagnosis(diagnosis)
                    .similarQuestions(similar)
                    .build();
            details.add(detail);
        }

        double accuracy = questions.isEmpty() ? 0 : (double) correctCount / questions.size();
        jdbcTemplate.update(
                "INSERT INTO grading_report (id, user_id, total_score, max_score, correct_count, wrong_count, accuracy, subject) " +
                "VALUES (?,?,?,?,?,?,?,?)",
                reportId, userId, totalScore, maxScore, correctCount, wrongCount, accuracy, subjectType);

        for (QuestionDetail detail : details) {
            saveQuestionResult(reportId, detail);
        }

        onStep.accept(Map.of("step", "complete", "reportId", reportId));

        return GradingReportResponse.builder()
                .reportId(reportId).totalScore(totalScore).maxScore(maxScore)
                .correctCount(correctCount).wrongCount(wrongCount).accuracy(accuracy)
                .questions(details).build();
    }

    private List<ParsedQuestion> extractQuestions(String content) {
        List<ParsedQuestion> questions = new ArrayList<>();
        String[] parts = content.split("\\n(?=\\d+[\\.、])");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty() || trimmed.length() < 5) continue;

            String[] qa = trimmed.split("[答解][：:]|答案[：:]", 2);
            String question = qa[0].trim();
            String answer = qa.length > 1 ? qa[1].trim() : "";
            if (answer.isEmpty() && qa.length > 1 && qa[1].contains("答")) {
                answer = qa[1].substring(qa[1].indexOf("答") + 1).trim();
            }
            questions.add(new ParsedQuestion(question, answer));
        }
        return questions;
    }

    private String buildGradingPrompt(String question, String studentAnswer, String subject) {
        return String.format("""
            你是%s教师。请批改以下题目，给出正确/错误判断和正确答案。

            题目：%s
            学生答案：%s

            请用以下JSON格式回复（只回复JSON，不要其他内容）：
            {"isCorrect": true/false, "correctAnswer": "正确答案", "knowledgePoint": "考点名称", "explanation": "简短解析"}
            """, subject != null ? subject : "学科", question, studentAnswer);
    }

    private boolean isCorrectAnswer(String gradingResult) {
        return gradingResult != null && gradingResult.contains("\"isCorrect\": true");
    }

    private String extractCorrectAnswer(String gradingResult) {
        try {
            int start = gradingResult.indexOf("\"correctAnswer\": \"");
            if (start > 0) {
                start += 19;
                int end = gradingResult.indexOf("\"", start);
                return gradingResult.substring(start, end);
            }
        } catch (Exception e) {
            log.warn("提取正确答案失败: gradingResult={}", gradingResult, e);
        }
        return "";
    }

    private String extractKnowledgePoint(String gradingResult) {
        try {
            int start = gradingResult.indexOf("\"knowledgePoint\": \"");
            if (start > 0) {
                start += 21;
                int end = gradingResult.indexOf("\"", start);
                return gradingResult.substring(start, end);
            }
        } catch (Exception e) {
            log.warn("提取知识点失败: gradingResult={}", gradingResult, e);
        }
        return "未分类";
    }

    private List<String> extractKnowledgePoints(String gradingResult) {
        String kp = extractKnowledgePoint(gradingResult);
        return kp.isEmpty() ? List.of() : List.of(kp);
    }

    private void saveQuestionResult(String reportId, QuestionDetail detail) {
        try {
            jdbcTemplate.update("DELETE FROM grading_question_result WHERE id = ?", detail.getId());
            jdbcTemplate.update(
                    "INSERT INTO grading_question_result (id, report_id, question_index, question_text, " +
                    "student_answer, correct_answer, is_correct, score, max_score, question_type, " +
                    "knowledge_point, diagnosis_json, similar_questions) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?::jsonb,?::jsonb)",
                    detail.getId(), reportId, detail.getIndex(), detail.getQuestionText(),
                    detail.getStudentAnswer(), detail.getCorrectAnswer(), detail.isCorrect(),
                    detail.getScore(), detail.getMaxScore(), "subjective",
                    detail.getKnowledgePoint(),
                    detail.getDiagnosis() != null ? objectMapper.writeValueAsString(detail.getDiagnosis()) : null,
                    detail.getSimilarQuestions() != null ? objectMapper.writeValueAsString(detail.getSimilarQuestions()) : null);
        } catch (Exception e) {
            log.error("保存批改结果失败", e);
        }
    }

    @Override
    public String diagnoseWrongQuestions(List<Map<String, Object>> questions,
                                          long userId, String subject,
                                          Consumer<Map<String, Object>> onStep) {
        String reportId = UUID.randomUUID().toString().substring(0, 16);
        for (int i = 0; i < questions.size(); i++) {
            Map<String, Object> q = questions.get(i);
            if (Boolean.TRUE.equals(q.get("isCorrect"))) continue;
            onStep.accept(Map.of("step", "diagnose", "current", i + 1, "total", questions.size()));
            try {
                String questionText = (String) q.getOrDefault("questionText", "");
                String studentAnswer = (String) q.getOrDefault("studentAnswer", "");
                String correctAnswer = (String) q.getOrDefault("correctAnswer", "");
                String kp = (String) q.getOrDefault("knowledgePoint", "");

                var diagResults = diagnosisService.diagnose(
                        questionText, studentAnswer, subject,
                        String.valueOf(userId), (nodeId, text) -> {}, r -> {});
                Map<String, Object> diagnosis = new HashMap<>();
                for (var entry : diagResults.entrySet()) {
                    diagnosis.put(entry.getKey(),
                            entry.getValue().getOutput() != null
                                    ? entry.getValue().getOutput().get("content") : "");
                }
                q.put("diagnosis", diagnosis);

                List<String> tags = kp.isEmpty() ? List.of() : List.of(kp);
                List<Map<String, Object>> similar = recallService.recall(
                        questionText, tags, 3).stream()
                        .map(r -> Map.<String, Object>of(
                                "id", r.id(), "text", r.questionText(),
                                "tags", r.tags(), "score", r.rrfScore()))
                        .toList();
                q.put("similarQuestions", similar);

                errorBookService.addFromGrading(
                        userId, reportId,
                        questionText, studentAnswer,
                        correctAnswer, kp,
                        subject, detectErrorTypeFromKp(kp),
                        diagnosis, similar);
            } catch (Exception e) {
                log.warn("错题诊断失败: index={}", i, e);
            }
        }
        return reportId;
    }

    @Override
    public void saveReport(String reportId, long userId, String subject, int maxScore, int totalScore,
                            int correctCount, int wrongCount, double accuracy,
                            List<Map<String, Object>> questions) {
        jdbcTemplate.update(
                "INSERT INTO grading_report (id, user_id, total_score, max_score, correct_count, wrong_count, accuracy, subject) " +
                "VALUES (?,?,?,?,?,?,?,?)",
                reportId, userId, totalScore, maxScore, correctCount, wrongCount, accuracy, subject);
        if (questions != null) {
            for (int i = 0; i < questions.size(); i++) {
                Map<String, Object> q = questions.get(i);
                try {
                    String qId = reportId + "-q" + (i + 1);
                    String questionText = (String) q.getOrDefault("questionText", "");
                    String studentAnswer = (String) q.getOrDefault("studentAnswer", "");
                    String correctAnswer = (String) q.getOrDefault("correctAnswer", "");
                    boolean isCorrect = Boolean.TRUE.equals(q.get("isCorrect"));
                    int score = q.get("score") instanceof Number n ? n.intValue() : 0;
                    int qMaxScore = q.get("maxScore") instanceof Number n ? n.intValue() : maxScore;
                    String kp = (String) q.getOrDefault("knowledgePoint", "未分类");
                    jdbcTemplate.update("DELETE FROM grading_question_result WHERE id = ?", qId);
                    Map<String, Object> diagnosis = (Map<String, Object>) q.get("diagnosis");
                    List<Map<String, Object>> similar = (List<Map<String, Object>>) q.get("similarQuestions");
                    jdbcTemplate.update(
                            "INSERT INTO grading_question_result (id, report_id, question_index, question_text, " +
                            "student_answer, correct_answer, is_correct, score, max_score, question_type, knowledge_point, " +
                            "diagnosis_json, similar_questions) " +
                            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?::jsonb,?::jsonb)",
                            qId, reportId, i + 1, questionText, studentAnswer, correctAnswer,
                            isCorrect, score, qMaxScore, "subjective", kp,
                            diagnosis != null ? objectMapper.writeValueAsString(diagnosis) : null,
                            similar != null ? objectMapper.writeValueAsString(similar) : null);
                } catch (Exception e) {
                    log.warn("保存批改题目结果失败: index={}", i, e);
                }
            }
        }
    }

    private String detectErrorType(String gradingResult) {
        if (gradingResult == null) return "unknown";
        String lower = gradingResult.toLowerCase();
        if (lower.contains("公式") || lower.contains("混淆")) return "formula_confusion";
        if (lower.contains("概念") || lower.contains("定义") || lower.contains("理解")) return "concept_gap";
        if (lower.contains("计算") || lower.contains("算术") || lower.contains("代入")) return "calculation_error";
        return "knowledge_gap";
    }

    private String detectErrorTypeFromKp(String knowledgePoint) {
        if (knowledgePoint == null || knowledgePoint.isEmpty()) return "knowledge_gap";
        String lower = knowledgePoint.toLowerCase();
        if (lower.contains("公式") || lower.contains("混淆")) return "formula_confusion";
        if (lower.contains("概念") || lower.contains("定义") || lower.contains("理解")) return "concept_gap";
        if (lower.contains("计算") || lower.contains("算术") || lower.contains("代入")) return "calculation_error";
        return "knowledge_gap";
    }

    private record ParsedQuestion(String question, String studentAnswer) {}
}
