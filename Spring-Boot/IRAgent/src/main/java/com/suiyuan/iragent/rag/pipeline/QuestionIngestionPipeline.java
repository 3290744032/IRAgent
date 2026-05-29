package com.suiyuan.iragent.rag.pipeline;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suiyuan.iragent.rag.embedding.EmbeddingService;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.dml.InsertParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;

/**
 * 真题入库 Pipeline: JSON 文件 → Embedding → Milvus 向量库 + PostgreSQL 全文检索双写。
 */
@Slf4j
@Component
public class QuestionIngestionPipeline {

    private final EmbeddingService embeddingService;
    private final MilvusServiceClient milvusClient;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    public QuestionIngestionPipeline(EmbeddingService embeddingService,
                                      MilvusServiceClient milvusClient,
                                      ObjectMapper objectMapper,
                                      JdbcTemplate jdbcTemplate) {
        this.embeddingService = embeddingService;
        this.milvusClient = milvusClient;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 从 JSON 文件批量导入真题
     * @param jsonFile 真题 JSON 文件
     * @return 导入的题目数量
     */
    public int ingest(File jsonFile) throws Exception {
        List<QuestionRecord> records = objectMapper.readValue(jsonFile,
                new TypeReference<>() {});
        return ingest(records);
    }

    public int ingest(List<QuestionRecord> records) {
        if (records.isEmpty()) return 0;

        // 批量提取文本
        List<String> texts = records.stream().map(QuestionRecord::getText).toList();

        // 批量 Embedding
        log.info("开始批量 Embedding: {} 条题目", texts.size());
        List<float[]> embeddings = embeddingService.embedBatch(texts);
        log.info("Embedding 完成: {} 条", embeddings.size());

        if (embeddings.size() != texts.size()) {
            log.warn("Embedding 返回数不匹配: expected={}, actual={}", texts.size(), embeddings.size());
        }

        // 组装 Milvus 插入数据
        List<InsertParam.Field> fields = new ArrayList<>();
        List<String> textField = new ArrayList<>();
        List<List<Float>> embField = new ArrayList<>();
        List<String> tagsField = new ArrayList<>();
        List<String> provinceField = new ArrayList<>();
        List<Integer> yearField = new ArrayList<>();

        for (int i = 0; i < Math.min(records.size(), embeddings.size()); i++) {
            QuestionRecord r = records.get(i);
            textField.add(r.getText());
            List<Float> vec = new ArrayList<>();
            for (float v : embeddings.get(i)) vec.add(v);
            embField.add(vec);
            tagsField.add(r.getTagsJson());
            provinceField.add(r.getProvince() != null ? r.getProvince() : "");
            yearField.add(r.getYear());
        }

        fields.add(new InsertParam.Field("question_text", textField));
        fields.add(new InsertParam.Field("embedding", embField));
        fields.add(new InsertParam.Field("tags", tagsField));
        fields.add(new InsertParam.Field("province", provinceField));
        fields.add(new InsertParam.Field("year", yearField));

        InsertParam param = InsertParam.newBuilder()
                .withCollectionName("exam_questions")
                .withFields(fields)
                .build();

        var result = milvusClient.insert(param);
        log.info("Milvus 入库: {} 条, status={}", textField.size(), result.getStatus());

        // 入库后 flush 确保数据可检索
        milvusClient.flush(io.milvus.param.collection.FlushParam.newBuilder()
                .withCollectionNames(List.of("exam_questions")).build());
        log.info("Milvus flush 完成");

        // 同步写入 PostgreSQL（全文检索用）
        syncToPostgres(records);

        return textField.size();
    }

    private void syncToPostgres(List<QuestionRecord> records) {
        String sql = "INSERT INTO question (id, question_text, tags, province, year) VALUES (?,?,?,?,?) " +
                     "ON CONFLICT (id) DO NOTHING";
        List<Object[]> batchArgs = new ArrayList<>();
        for (QuestionRecord r : records) {
            batchArgs.add(new Object[]{
                    UUID.randomUUID().toString().substring(0, 16),
                    r.getText(),
                    r.getTagsJson(),
                    r.getProvince() != null ? r.getProvince() : "",
                    r.getYear()
            });
        }
        jdbcTemplate.batchUpdate(sql, batchArgs);
        log.info("PostgreSQL 同步写入: {} 条", records.size());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            var listResult = milvusClient.showCollections(
                    io.milvus.param.collection.ShowCollectionsParam.newBuilder().build());
            stats.put("collections", listResult.getData().getCollectionNamesList());
            stats.put("collectionCount", listResult.getData().getCollectionNamesCount());
        } catch (Exception e) {
            stats.put("error", e.getMessage());
        }
        return stats;
    }

    /**
     * 真题数据记录
     */
    public record QuestionRecord(
            String text,
            String tagsJson,
            String province,
            int year
    ) {
        public String getText() { return text; }
        public String getTagsJson() { return tagsJson; }
        public String getProvince() { return province; }
        public int getYear() { return year; }
    }
}
