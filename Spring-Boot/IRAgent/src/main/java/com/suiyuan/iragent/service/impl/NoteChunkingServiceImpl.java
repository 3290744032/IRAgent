package com.suiyuan.iragent.service.impl;

import com.suiyuan.iragent.service.NoteChunkingService;
import com.suiyuan.iragent.service.NoteChunkingService.Chunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 改进版笔记分块服务 — 解决碎片化和知识点错位问题
 * <p>
 * 策略：
 * 1. 按所有级别 Markdown 标题（# ~ ######）切分
 * 2. 标题作为知识点标签，清洗序号和标点
 * 3. 提高最小块阈值到 80 字符，减少碎片
 * 4. 合并过短的相邻 chunk，保持语义完整性
 */
@Slf4j
@Service
public class NoteChunkingServiceImpl implements NoteChunkingService {

    // 匹配任意级别的 Markdown 标题
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);
    private static final int MIN_CHUNK_LENGTH = 100;
    private static final int MAX_CHUNK_LENGTH = 1200;

    public List<Chunk> chunk(String content) {
        if (content == null || content.trim().isEmpty()) return List.of();

        List<Chunk> chunks = new ArrayList<>();
        content = cleanContent(content);

        // 正则一次性匹配所有标题位置，避免错位
        var matcher = HEADING_PATTERN.matcher(content);
        int lastEnd = 0;
        String currentKp = "未分类";

        while (matcher.find()) {
            // 处理上一个标题区间的内容
            if (lastEnd < matcher.start()) {
                String sectionContent = content.substring(lastEnd, matcher.start()).trim();
                if (sectionContent.length() >= MIN_CHUNK_LENGTH) {
                    chunks.add(new Chunk(cleanKnowledgePoint(currentKp), sectionContent));
                }
            }
            // 更新当前知识点（从标题提取）
            currentKp = matcher.group(2).trim();
            lastEnd = matcher.end();
        }

        // 处理最后一个标题之后的内容
        if (lastEnd < content.length()) {
            String lastSection = content.substring(lastEnd).trim();
            if (lastSection.length() >= MIN_CHUNK_LENGTH) {
                chunks.add(new Chunk(cleanKnowledgePoint(currentKp), lastSection));
            }
        }

        // 兜底：整篇作为一块
        if (chunks.isEmpty() && content.length() >= MIN_CHUNK_LENGTH) {
            chunks.add(new Chunk("全文", content.trim()));
        }

        // 后处理：合并过短的连续 chunk
        chunks = mergeShortChunks(chunks);

        log.debug("笔记分块完成: {} 块", chunks.size());
        return chunks;
    }

    // ==================== 辅助方法 ====================

    /** 文本预处理：归一化空白 + 限制连续换行 */
    private String cleanContent(String text) {
        return text
                .replaceAll("\\s+", " ")
                .replaceAll("(?m)^\\s*[•\\-•]\\s*", "• ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    /** 知识点标签清洗：去序号→去标点→截断最多28字 */
    private String cleanKnowledgePoint(String kp) {
        if (kp == null || kp.isBlank()) return "未分类";
        return kp
                .replaceAll("^\\d+[.、．)]\\s*", "")           // 去 "1." 等序号
                .replaceAll("^[一二三四五六七八九十]+[、．)]\\s*", "") // 去 "一、" 等
                .replaceAll("[：:；;。,.！!?？]\\s*$", "")       // 去末尾标点
                .replaceAll("\\s+", " ")
                .trim()
                .replaceFirst("^(.{0,28}).*$", "$1");          // 最多28字
    }

    /** 合并过短的连续 chunk（相同知识点），避免碎片化 */
    private List<Chunk> mergeShortChunks(List<Chunk> chunks) {
        if (chunks.size() <= 1) return chunks;

        List<Chunk> merged = new ArrayList<>();
        StringBuilder tempContent = new StringBuilder();
        String tempKp = chunks.get(0).knowledgePoint();

        for (Chunk chunk : chunks) {
            boolean sameKp = tempKp.equals(chunk.knowledgePoint());
            boolean tooShort = chunk.content().length() < 150;

            if (sameKp || tooShort) {
                if (tempContent.length() > 0) tempContent.append("\n\n");
                tempContent.append(chunk.content());
                if (!sameKp) tempKp = chunk.knowledgePoint();
            } else {
                String finalContent = tempContent.toString().trim();
                if (finalContent.length() >= MIN_CHUNK_LENGTH) {
                    merged.add(new Chunk(tempKp, finalContent));
                }
                tempContent = new StringBuilder(chunk.content());
                tempKp = chunk.knowledgePoint();
            }

            // 单个块太长就切出去
            if (tempContent.length() > MAX_CHUNK_LENGTH) {
                merged.add(new Chunk(tempKp, tempContent.toString().trim()));
                tempContent.setLength(0);
            }
        }

        // 收尾
        if (tempContent.length() > 0) {
            String finalContent = tempContent.toString().trim();
            if (finalContent.length() >= MIN_CHUNK_LENGTH) {
                merged.add(new Chunk(tempKp, finalContent));
            }
        }

        return merged;
    }
}
