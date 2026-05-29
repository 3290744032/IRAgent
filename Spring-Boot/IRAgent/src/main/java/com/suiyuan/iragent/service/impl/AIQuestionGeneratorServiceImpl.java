package com.suiyuan.iragent.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suiyuan.iragent.config.VolcEngineChatClient;
import com.suiyuan.iragent.service.AIQuestionGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIQuestionGeneratorServiceImpl implements AIQuestionGeneratorService {

    private final VolcEngineChatClient chatClient;
    private final StringRedisTemplate redis;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public Map<String, Object> generateOne(QuestionContext ctx) {
        String cacheKey = buildCacheKey(ctx);
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                log.debug("AI 出题缓存命中: kp={}, type={}, diff={}", ctx.knowledgePoint(), ctx.questionType(), ctx.difficulty());
                return objectMapper.readValue(cached, new TypeReference<>() {});
            } catch (Exception e) {
                log.warn("AI 出题缓存反序列化失败", e);
            }
        }

        Map<String, Object> similar = findSimilarInDb(ctx);
        if (similar != null) {
            log.info("AI 出题语义命中: kp={}, type={}, reused id={}",
                    ctx.knowledgePoint(), ctx.questionType(), similar.get("id"));
            return similar;
        }

        Map<String, Object> question = callLLM(ctx);
        if (question != null && !question.isEmpty()) {
            String qid = "aiq-" + UUID.randomUUID().toString().substring(0, 12);
            question.put("id", qid);
            question.put("source", "ai-generated");

            persistToDb(qid, question, ctx);
            try {
                redis.opsForValue().set(cacheKey, objectMapper.writeValueAsString(question), 7, TimeUnit.DAYS);
            } catch (Exception e) { log.warn("AI 题目缓存写入失败: {}", e.getMessage()); }
            log.info("AI 生成题目: kp={}, type={}, qid={}", ctx.knowledgePoint(), ctx.questionType(), qid);
        }
        return question;
    }

    public List<Map<String, Object>> generateBatch(List<QuestionContext> contexts) {
        return contexts.stream().map(this::generateOne).filter(Objects::nonNull).toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callLLM(QuestionContext ctx) {
        String prompt = buildPrompt(ctx);
        try {
            String response = chatClient.chat(prompt);
            int start = response.indexOf("{");
            int end = response.lastIndexOf("}") + 1;
            if (start >= 0 && end > start) {
                return objectMapper.readValue(response.substring(start, end), new TypeReference<>() {});
            }
        } catch (Exception e) {
            log.error("AI 出题失败: kp={}, error={}", ctx.knowledgePoint(), e.getMessage());
        }
        return null;
    }

    private String buildPrompt(QuestionContext ctx) {
        return String.format("""
            你是一位%s命题专家。请生成一道满足以下条件的题目，严格按 JSON 格式输出。

            考点：%s
            题型：%s
            难度：%d/5
            目标考试：%s

            %s

            要求：
            1. 题干清晰无歧义
            2. 如果是选择题，4个选项的干扰项要有迷惑性
            3. 提供完整的解析过程
            4. 题目必须符合该考试的考纲范围
            5. JSON 格式：{"questionText":"...","questionType":"%s","options":[...],"correctAnswer":"...","explanation":"...","difficulty":%d,"knowledgePoint":"%s","tags":[...]}
            6. 不要输出任何 JSON 之外的文字
            """,
                ctx.subject(), ctx.knowledgePoint(), ctx.questionType(), ctx.difficulty(),
                ctx.examType() != null ? ctx.examType() : "通用",
                ctx.notes() != null ? "学生笔记参考：\n" + ctx.notes() : "",
                ctx.questionType(), ctx.difficulty(), ctx.knowledgePoint());
    }

    private Map<String, Object> findSimilarInDb(QuestionContext ctx) {
        try {
            var rows = jdbcTemplate.queryForList("""
                    SELECT id, question_text AS "questionText", question_type AS "questionType",
                           options, correct_answer AS "correctAnswer", explanation,
                           difficulty, knowledge_point AS "knowledgePoint", tags, source
                    FROM question
                    WHERE knowledge_point = ?
                      AND question_type = ?
                      AND ABS(difficulty - ?) <= 1
                      AND source = 'ai-generated'
                    ORDER BY RANDOM()
                    LIMIT 1
                    """, ctx.knowledgePoint(), ctx.questionType(), ctx.difficulty());

            if (!rows.isEmpty()) {
                var row = rows.get(0);
                if (row.get("options") instanceof String optStr) {
                    try {
                        row.put("options", objectMapper.readValue(optStr, List.class));
                    } catch (Exception e) {
                        log.warn("解析 options JSON 失败: {}", optStr, e);
                    }
                }
                if (row.get("tags") instanceof String tagStr) {
                    try {
                        row.put("tags", objectMapper.readValue(tagStr, List.class));
                    } catch (Exception e) {
                        log.warn("解析 tags JSON 失败: {}", tagStr, e);
                    }
                }
                row.put("source", "ai-generated");
                return row;
            }
        } catch (Exception e) {
            log.debug("PG 近似匹配失败: {}", e.getMessage());
        }
        return null;
    }

    private String buildCacheKey(QuestionContext ctx) {
        String raw = ctx.knowledgePoint() + "|" + ctx.questionType() + "|" + ctx.difficulty() + "|" +
                     (ctx.examType() != null ? ctx.examType() : "");
        return "genq:" + md5(raw);
    }

    private void persistToDb(String qid, Map<String, Object> q, QuestionContext ctx) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO question (id, question_text, question_type, options, correct_answer, " +
                    "explanation, difficulty, subject, chapter, knowledge_point, tags, year, exam_type, source) " +
                    "VALUES (?,?,?,?::jsonb,?,?,?,?,?,?,?::jsonb,?,?,?) ON CONFLICT (id) DO NOTHING",
                    qid, q.get("questionText"), ctx.questionType(),
                    q.get("options") != null ? objectMapper.writeValueAsString(q.get("options")) : null,
                    q.get("correctAnswer"), q.get("explanation"), ctx.difficulty(),
                    ctx.subject(), ctx.chapter(), ctx.knowledgePoint(),
                    q.get("tags") != null ? objectMapper.writeValueAsString(q.get("tags")) : "[]",
                    ctx.year(), ctx.examType(), "ai-generated");
        } catch (Exception e) {
            log.warn("AI 题目入库失败: qid={}, error={}", qid, e.getMessage());
        }
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return Integer.toHexString(input.hashCode()); }
    }
}
