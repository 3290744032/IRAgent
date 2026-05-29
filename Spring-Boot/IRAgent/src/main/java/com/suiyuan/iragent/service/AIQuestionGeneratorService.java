package com.suiyuan.iragent.service;

import java.util.List;
import java.util.Map;

public interface AIQuestionGeneratorService {

    /**
     * 生成一道题目（带缓存）。
     */
    Map<String, Object> generateOne(AIQuestionGeneratorService.QuestionContext ctx);

    /**
     * 批量生成题目。
     */
    List<Map<String, Object>> generateBatch(List<AIQuestionGeneratorService.QuestionContext> contexts);

    /**
     * 出题上下文——AI 需要知道的所有信息来生成个性化题目。
     */
    record QuestionContext(
            String subject, String chapter, String knowledgePoint,
            String questionType, int difficulty, String examType,
            Integer year, String notes
    ) {}
}
