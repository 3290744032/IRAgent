package com.suiyuan.iragent.rag.retrieval;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * PostgreSQL 全文检索——tsvector + GIN 索引 + LIKE 中文降级。
 *
 * tsvector('simple') 对中文分词有限，LIKE 降级确保中文关键词也能召回。
 */
@Slf4j
@Service
public class FulltextSearchService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public FulltextSearchService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<SearchResult> search(String query, int topK) {
        // 1. 先尝试 tsvector 全文检索
        List<SearchResult> results = tsvectorSearch(query, topK);
        if (!results.isEmpty()) return results;

        // 2. 无结果则降级为 LIKE 模糊匹配（中文兼容）
        log.debug("tsvector 无结果，降级为 LIKE 查询");
        return likeSearch(query, topK);
    }

    private List<SearchResult> tsvectorSearch(String query, int topK) {
        try {
            String sql = """
                SELECT id, question_text, tags, province, year,
                       ts_rank(to_tsvector('simple', question_text), plainto_tsquery('simple', ?)) AS rank
                FROM question
                WHERE to_tsvector('simple', question_text) @@ plainto_tsquery('simple', ?)
                ORDER BY rank DESC
                LIMIT ?
                """;
            return jdbcTemplate.query(sql,
                    ps -> { ps.setString(1, query); ps.setString(2, query); ps.setInt(3, topK); },
                    (rs, rowNum) -> mapRow(rs, rs.getDouble("rank")));
        } catch (Exception e) {
            log.debug("tsvector 查询失败: {}", e.getMessage());
            return List.of();
        }
    }

    private List<SearchResult> likeSearch(String query, int topK) {
        // 按空格和标点拆关键词
        String[] keywords = query.split("[\\s,，、。；;]+");
        StringBuilder sql = new StringBuilder(
                "SELECT id, question_text, tags, province, year FROM question WHERE ");
        List<String> params = new ArrayList<>();

        for (int i = 0; i < keywords.length; i++) {
            if (i > 0) sql.append(" OR ");
            sql.append("question_text LIKE ?");
            params.add("%" + keywords[i] + "%");
        }
        sql.append(" LIMIT ?");

        List<Object> paramObjs = new ArrayList<>(params);
        paramObjs.add(topK);

        return jdbcTemplate.query(sql.toString(),
                ps -> {
                    for (int i = 0; i < paramObjs.size(); i++) {
                        if (paramObjs.get(i) instanceof Integer n) ps.setInt(i + 1, n);
                        else ps.setString(i + 1, (String) paramObjs.get(i));
                    }
                },
                (rs, rowNum) -> mapRow(rs, 1.0 / (rowNum + 1)));
    }

    private SearchResult mapRow(java.sql.ResultSet rs, double score) throws java.sql.SQLException {
        return new SearchResult(
                rs.getString("id"),
                rs.getString("question_text"),
                score, "fulltext",
                parseTags(rs.getString("tags")),
                rs.getString("province"),
                rs.getInt("year"));
    }

    @SuppressWarnings("unchecked")
    private List<String> parseTags(String tagsJson) {
        try {
            return objectMapper.readValue(tagsJson, List.class);
        } catch (Exception e) {
            return List.of();
        }
    }

    public record SearchResult(
            String id, String questionText, double score, String source,
            List<String> tags, String province, int year) {}
}
