package com.suiyuan.iragent.rag.retrieval;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * RRF (Reciprocal Rank Fusion) 多路召回融合重排。
 *
 * 公式: score(doc) = Σ weight_i / (k + rank_i(doc))
 * 默认 k=60，两路权重默认各 0.5。
 *
 * 面试点：向量检索语义相似 + 全文检索关键词精确匹配 → 双路互补。
 */
@Slf4j
@Component
public class RrfRanker {

    private static final double K = 60.0;

    /**
     * 双路召回融合重排
     * @param vectorResults Milvus 向量检索结果
     * @param fulltextResults PostgreSQL 全文检索结果
     * @param topK 最终返回数量
     * @return 融合重排后的结果
     */
    public List<RankedResult> merge(
            List<FulltextSearchService.SearchResult> vectorResults,
            List<FulltextSearchService.SearchResult> fulltextResults,
            int topK) {

        return merge(vectorResults, fulltextResults, topK, 0.5, 0.5);
    }

    public List<RankedResult> merge(
            List<FulltextSearchService.SearchResult> vectorResults,
            List<FulltextSearchService.SearchResult> fulltextResults,
            int topK, double vectorWeight, double fulltextWeight) {

        Map<String, Double> scores = new LinkedHashMap<>();
        Map<String, FulltextSearchService.SearchResult> docs = new LinkedHashMap<>();

        // 向量路
        accumulate(scores, docs, vectorResults, vectorWeight);
        // 全文路
        accumulate(scores, docs, fulltextResults, fulltextWeight);

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> new RankedResult(
                        e.getKey(),
                        docs.get(e.getKey()).questionText(),
                        e.getValue(),
                        docs.get(e.getKey()).tags(),
                        docs.get(e.getKey()).province(),
                        docs.get(e.getKey()).year()
                ))
                .toList();
    }

    private void accumulate(Map<String, Double> scores,
                            Map<String, FulltextSearchService.SearchResult> docs,
                            List<FulltextSearchService.SearchResult> results,
                            double weight) {
        for (int i = 0; i < results.size(); i++) {
            FulltextSearchService.SearchResult r = results.get(i);
            scores.merge(r.id(), weight / (K + i + 1), Double::sum);
            docs.putIfAbsent(r.id(), r);
        }
    }

    public record RankedResult(
            String id,
            String questionText,
            double rrfScore,
            List<String> tags,
            String province,
            int year
    ) {}
}
