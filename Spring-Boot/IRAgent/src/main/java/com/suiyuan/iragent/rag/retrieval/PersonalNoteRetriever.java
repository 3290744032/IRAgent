package com.suiyuan.iragent.rag.retrieval;

import com.suiyuan.iragent.rag.embedding.EmbeddingService;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.MetricType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.response.SearchResultsWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 个人笔记语义检索——在 per-user Milvus Collection 中搜索相关笔记片段。
 * Collection 命名: kb_notes_{userId}
 */
@Slf4j
@Service
public class PersonalNoteRetriever {

    private final EmbeddingService embeddingService;
    private final MilvusServiceClient milvusClient;

    public PersonalNoteRetriever(EmbeddingService embeddingService,
                                  MilvusServiceClient milvusClient) {
        this.embeddingService = embeddingService;
        this.milvusClient = milvusClient;
    }

    public List<NoteFragment> search(long userId, String query, int topK) {
        String collName = "kb_notes_" + userId;

        // 检查 Collection 是否存在
        var hasResult = milvusClient.hasCollection(
                HasCollectionParam.newBuilder().withCollectionName(collName).build());
        if (hasResult.getData() == null || !hasResult.getData()) {
            return List.of();
        }

        float[] embedding = embeddingService.embed(query);
        if (embedding.length == 0) return List.of();

        List<Float> vec = new ArrayList<>();
        for (float v : embedding) vec.add(v);

        var searchResult = milvusClient.search(
                SearchParam.newBuilder()
                        .withCollectionName(collName)
                        .withMetricType(MetricType.L2)
                        .withTopK(topK)
                        .withVectorFieldName("embedding")
                        .withFloatVectors(List.of(vec))
                        .withParams("{\"ef\": 64}")
                        .addOutField("content")
                        .build());

        if (searchResult.getData() == null) return List.of();

        SearchResultsWrapper wrapper = new SearchResultsWrapper(
                searchResult.getData().getResults());

        List<NoteFragment> fragments = new ArrayList<>();
        List<SearchResultsWrapper.IDScore> scores = wrapper.getIDScore(0);

        for (SearchResultsWrapper.IDScore score : scores) {
            String content = String.valueOf(score.get("content"));
            double similarity = 1.0 / (1.0 + score.getScore());
            fragments.add(new NoteFragment(score.getLongID(), content, similarity));
        }

        log.debug("个人笔记检索: userId={}, query={}, hits={}", userId,
                query.substring(0, Math.min(20, query.length())), fragments.size());
        return fragments;
    }

    public record NoteFragment(long milvusId, String content, double similarity) {}
}
