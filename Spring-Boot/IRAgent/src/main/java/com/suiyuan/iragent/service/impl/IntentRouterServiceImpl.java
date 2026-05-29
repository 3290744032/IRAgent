package com.suiyuan.iragent.service.impl;

import com.suiyuan.iragent.service.IntentRouterService;
import com.suiyuan.iragent.service.IntentRouterService.Intent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * 意图路由器——基于关键词 + 规则引擎判断用户意图。
 *
 * HINT_NEEDED: 学生请求引导而非直接答案
 * FULL_EXPLANATION: 需要完整解答
 * NOTE_SEARCH: 想查自己的笔记
 * PRACTICE_READY: 已经掌握，想刷题
 */
@Slf4j
@Service
public class IntentRouterServiceImpl implements IntentRouterService {

    private static final Set<String> HINT_KEYWORDS = Set.of(
            "提示", "引导", "帮我一步步", "不太会", "怎么做", "卡住了",
            "思路", "教教我", "讲一下", "不会做", "hint"
    );

    private static final Set<String> NOTE_KEYWORDS = Set.of(
            "笔记", "我记得", "我写过", "查看笔记", "我的笔记", "找一下笔记"
    );

    private static final Set<String> PRACTICE_KEYWORDS = Set.of(
            "刷题", "练习", "来几道题", "有类似的题吗", "再出几道",
            "给我题", "来道题", "同类题"
    );

    public Intent detect(String userMessage) {
        if (userMessage == null) return Intent.FULL_EXPLANATION;
        String lower = userMessage.toLowerCase();

        long hintScore = countMatches(lower, HINT_KEYWORDS);
        long noteScore = countMatches(lower, NOTE_KEYWORDS);
        long practiceScore = countMatches(lower, PRACTICE_KEYWORDS);

        if (noteScore > 0) return Intent.NOTE_SEARCH;
        if (practiceScore > hintScore && practiceScore > 0) return Intent.PRACTICE_READY;
        if (hintScore > 0) return Intent.HINT_NEEDED;
        return Intent.FULL_EXPLANATION;
    }

    public Map<String, Object> getModeConfig(Intent intent) {
        return switch (intent) {
            case HINT_NEEDED -> Map.of("mode", "socratic", "style", "引导式回答，不直接给答案");
            case NOTE_SEARCH -> Map.of("mode", "note_search", "style", "优先检索笔记原文");
            case PRACTICE_READY -> Map.of("mode", "practice", "style", "推荐同类变式题");
            default -> Map.of("mode", "full_explanation", "style", "完整解答并引用笔记");
        };
    }

    private long countMatches(String text, Set<String> keywords) {
        return keywords.stream().filter(text::contains).count();
    }
}
