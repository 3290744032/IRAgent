package com.suiyuan.iragent.service.impl;

import com.suiyuan.iragent.service.GraphDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 知识图谱数据 — 考点/笔记/错题三元拓扑
 * 节点名经过 cleanNodeName 清洗（去 Markdown/LaTeX/公式 → 2~12 字纯净名）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphDataServiceImpl implements GraphDataService {

    private final JdbcTemplate jdbcTemplate;

    public Map<String, Object> getGraphData(Long userId) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        Set<String> seenNodeIds = new HashSet<>();

        buildKpNodes(userId, nodes, seenNodeIds);

        Map<String, String> noteIdToNodeId = buildNoteNodes(userId, nodes, seenNodeIds);

        buildQuestionNodes(userId, nodes, seenNodeIds);

        buildKpNoteEdges(userId, nodes, noteIdToNodeId, edges);

        buildQuestionEdges(userId, nodes, noteIdToNodeId, seenNodeIds, edges);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nodes", nodes);
        result.put("edges", edges);
        result.put("nodeCount", nodes.size());
        result.put("edgeCount", edges.size());
        return result;
    }

    private void buildKpNodes(Long userId, List<Map<String, Object>> nodes, Set<String> seenNodeIds) {
        var kpRows = jdbcTemplate.queryForList("""
                SELECT nc.knowledge_point AS name,
                       COUNT(DISTINCT nc.note_id) AS note_count,
                       COALESCE(m.proficiency, 0.0) AS mastery
                FROM note_chunk nc
                LEFT JOIN mastery_records m
                  ON m.knowledge_point = nc.knowledge_point AND m.user_id = nc.user_id
                WHERE nc.user_id = ?
                  AND nc.knowledge_point IS NOT NULL
                  AND nc.knowledge_point != ''
                  AND nc.knowledge_point != '未分类'
                GROUP BY nc.knowledge_point, m.proficiency
                ORDER BY note_count DESC
                LIMIT 30
                """, userId);

        for (var row : kpRows) {
            String rawName = (String) row.get("name");
            String id = "kp_" + UUID.randomUUID().toString().substring(0, 8);
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", id);
            node.put("name", cleanNodeName(rawName));
            node.put("type", "knowledge_point");
            node.put("value", row.get("note_count"));
            node.put("mastery", row.get("mastery"));
            node.put("actualId", rawName);
            nodes.add(node);
            seenNodeIds.add(id);
        }
    }

    private Map<String, String> buildNoteNodes(Long userId, List<Map<String, Object>> nodes, Set<String> seenNodeIds) {
        var noteRows = jdbcTemplate.queryForList("""
                SELECT id, title, subject
                FROM note
                WHERE user_id = ?
                ORDER BY updated_at DESC
                LIMIT 30
                """, userId);

        Map<String, String> noteIdToNodeId = new LinkedHashMap<>();
        for (var row : noteRows) {
            String nodeId = "note_" + row.get("id");
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", nodeId);
            node.put("name", shorten((String) row.get("title"), 12));
            node.put("type", "note");
            node.put("value", 1);
            node.put("actualId", row.get("id"));
            node.put("subject", row.get("subject"));
            nodes.add(node);
            seenNodeIds.add(nodeId);
            noteIdToNodeId.put((String) row.get("id"), nodeId);
        }
        return noteIdToNodeId;
    }

    private void buildQuestionNodes(Long userId, List<Map<String, Object>> nodes, Set<String> seenNodeIds) {
        var qRows = jdbcTemplate.queryForList("""
                SELECT id, question_text, mastered, knowledge_point
                FROM error_book
                WHERE user_id = ?
                ORDER BY created_at DESC
                LIMIT 30
                """, userId);

        for (var row : qRows) {
            String nodeId = "q_" + row.get("id");
            String text = (String) row.get("question_text");
            String kp = (String) row.get("knowledge_point");
            String label;
            if (kp != null && !kp.isBlank() && !"未分类".equals(kp)) {
                label = cleanNodeName(kp);
            } else {
                label = text != null && text.length() > 8
                        ? text.substring(0, 7) + "…" : (text != null ? text : "题目");
            }
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", nodeId);
            node.put("name", label);
            node.put("type", "question");
            node.put("value", 1);
            node.put("actualId", row.get("id"));
            node.put("status", Boolean.TRUE.equals(row.get("mastered")) ? "correct" : "wrong");
            nodes.add(node);
            seenNodeIds.add(nodeId);
        }
    }

    private void buildKpNoteEdges(Long userId, List<Map<String, Object>> nodes,
                                   Map<String, String> noteIdToNodeId, List<Map<String, Object>> edges) {
        var chunkEdges = jdbcTemplate.queryForList("""
                SELECT DISTINCT nc.knowledge_point, nc.note_id
                FROM note_chunk nc
                WHERE nc.user_id = ?
                  AND nc.knowledge_point IS NOT NULL
                  AND nc.knowledge_point != ''
                  AND nc.knowledge_point != '未分类'
                """, userId);

        for (var row : chunkEdges) {
            String kpNodeId = findKpNode(nodes, (String) row.get("knowledge_point"));
            String noteNodeId = noteIdToNodeId.get((String) row.get("note_id"));
            if (kpNodeId != null && noteNodeId != null) {
                Map<String, Object> edge = new LinkedHashMap<>();
                edge.put("source", kpNodeId);
                edge.put("target", noteNodeId);
                edge.put("relation", "belongs_to");
                edges.add(edge);
            }
        }
    }

    private void buildQuestionEdges(Long userId, List<Map<String, Object>> nodes,
                                     Map<String, String> noteIdToNodeId, Set<String> seenNodeIds,
                                     List<Map<String, Object>> edges) {
        var qEdges = jdbcTemplate.queryForList("""
                SELECT DISTINCT eb.id AS question_id, nc.note_id, eb.knowledge_point
                FROM error_book eb
                JOIN note_chunk nc ON nc.knowledge_point = eb.knowledge_point AND nc.user_id = eb.user_id
                WHERE eb.user_id = ?
                """, userId);

        Set<String> addedQE = new HashSet<>();
        for (var row : qEdges) {
            String qNodeId = "q_" + row.get("question_id");
            String noteNodeId = noteIdToNodeId.get((String) row.get("note_id"));
            if (noteNodeId != null && seenNodeIds.contains(qNodeId)) {
                String key = qNodeId + "->" + noteNodeId;
                if (addedQE.add(key)) {
                    Map<String, Object> edge = new LinkedHashMap<>();
                    edge.put("source", noteNodeId);
                    edge.put("target", qNodeId);
                    edge.put("relation", "linked");
                    edges.add(edge);
                }
            }
            String kpNodeId = findKpNode(nodes, (String) row.get("knowledge_point"));
            if (kpNodeId != null && seenNodeIds.contains(qNodeId)) {
                String key2 = kpNodeId + "->" + qNodeId;
                if (addedQE.add(key2)) {
                    Map<String, Object> edge = new LinkedHashMap<>();
                    edge.put("source", kpNodeId);
                    edge.put("target", qNodeId);
                    edge.put("relation", "tests");
                    edges.add(edge);
                }
            }
        }
    }

    // ==================== 清洗 ====================

    private static String cleanNodeName(String raw) {
        if (raw == null || raw.isBlank()) return "未命名";
        String name = raw;
        int nl = name.indexOf('\n');
        if (nl > 0) name = name.substring(0, nl);
        name = name.trim()
                .replaceAll("^#{1,4}\\s*", "")
                .replaceAll("\\$\\$[\\s\\S]*?\\$\\$", "")
                .replaceAll("\\$[^$]+\\$", "")
                .replaceAll("\\\\[a-zA-Z]+(\\{[^}]*\\})*", "")
                .replaceAll("\\*\\*([^*]+)\\*\\*", "$1")
                .replaceAll("[{}\\\\]", "")
                .replaceAll("^[\\d]+[、.．)]\\s*", "")
                .replaceAll("\\s+", " ").trim();
        if (name.isEmpty()) name = raw.replaceAll("[\\$\\\\{}\\]\\[\\n\\r]", " ").replaceAll("\\s+", " ").trim();
        return shorten(name, 12);
    }

    private static String shorten(String s, int max) {
        if (s == null || s.isEmpty()) return "未命名";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private String findKpNode(List<Map<String, Object>> nodes, String kpName) {
        for (var node : nodes)
            if ("knowledge_point".equals(node.get("type")) && kpName.equals(node.get("actualId")))
                return (String) node.get("id");
        return null;
    }
}
