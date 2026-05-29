package com.suiyuan.iragent.rag.retrieval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suiyuan.iragent.rag.embedding.EmbeddingService;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.MetricType;
import io.milvus.param.dml.SearchParam;
import io.milvus.response.SearchResultsWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class AdaptiveRecallService {

    private final EmbeddingService embeddingService;
    private final MilvusServiceClient milvusClient;
    private final FulltextSearchService fulltextSearchService;
    private final RrfRanker rrfRanker;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdaptiveRecallService(EmbeddingService embeddingService,
                                  MilvusServiceClient milvusClient,
                                  FulltextSearchService fulltextSearchService,
                                  RrfRanker rrfRanker) {
        this.embeddingService = embeddingService;
        this.milvusClient = milvusClient;
        this.fulltextSearchService = fulltextSearchService;
        this.rrfRanker = rrfRanker;
    }

    public List<RrfRanker.RankedResult> recall(String query, List<String> weakTags, int topK) {
        float[] embedding = embeddingService.embed(query);
        if (embedding.length == 0) {
            List<FulltextSearchService.SearchResult> ftResults = fulltextSearchService.search(query, topK);
            return ftResults.stream()
                    .map(r -> new RrfRanker.RankedResult(r.id(), r.questionText(),
                            r.score(), r.tags(), r.province(), r.year()))
                    .toList();
        }

        List<FulltextSearchService.SearchResult> vectorResults = vectorSearch(embedding, weakTags, 20);
        List<FulltextSearchService.SearchResult> fulltextResults = fulltextSearchService.search(query, 20);
        List<RrfRanker.RankedResult> merged = rrfRanker.merge(vectorResults, fulltextResults, topK, 0.7, 0.3);

        log.info("自适应召回: vector={}, fulltext={}, merged={}",
                vectorResults.size(), fulltextResults.size(), merged.size());
        return merged;
    }

    private List<FulltextSearchService.SearchResult> vectorSearch(float[] embedding, List<String> weakTags, int topK) {
        List<Float> vec = new ArrayList<>();
        for (float v : embedding) vec.add(v);

        SearchParam.Builder builder = SearchParam.newBuilder()
                .withCollectionName("exam_questions")
                .withMetricType(MetricType.L2)
                .withTopK(topK)
                .withVectorFieldName("embedding")
                .withFloatVectors(List.of(vec))
                .withParams("{\"ef\": 128}")
                .withOutFields(List.of("question_text", "tags", "province", "year"));

        if (weakTags != null && !weakTags.isEmpty()) {
            StringBuilder filter = new StringBuilder();
            for (int i = 0; i < weakTags.size(); i++) {
                if (i > 0) filter.append(" or ");
                filter.append("tags like \"%").append(weakTags.get(i)).append("%\"");
            }
            builder.withExpr(filter.toString());
        }

        var response = milvusClient.search(builder.build());
        log.info("Milvus 检索状态: status={}, dataNull={}",
                response.getStatus(), response.getData() == null);

        if (response.getData() == null) {
            log.warn("Milvus 检索返回 null data");
            return List.of();
        }

        SearchResultsWrapper wrapper = new SearchResultsWrapper(
                response.getData().getResults());

        List<SearchResultsWrapper.IDScore> scores = wrapper.getIDScore(0);
        log.info("Milvus 检索结果: scoreCount={}", scores.size());
        List<FulltextSearchService.SearchResult> results = new ArrayList<>();

        for (SearchResultsWrapper.IDScore score : scores) {
            double similarity = 1.0 / (1.0 + score.getScore());
            String id = score.getStrID();
            if (id == null || id.isEmpty()) id = String.valueOf(score.getLongID());

            results.add(new FulltextSearchService.SearchResult(
                    id,
                    String.valueOf(score.get("question_text")),
                    similarity,
                    "milvus",
                    parseTags(String.valueOf(score.get("tags"))),
                    String.valueOf(score.get("province")),
                    parseYear(score.get("year"))));
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    private List<String> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank() || "null".equals(tagsJson)) return List.of();
        try {
            return objectMapper.readValue(tagsJson, List.class);
        } catch (Exception e) {
            return List.of(tagsJson);
        }
    }

    private int parseYear(Object yearObj) {
        if (yearObj == null) return 0;
        if (yearObj instanceof Number n) return n.intValue();
        if (yearObj instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }
}
