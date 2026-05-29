package com.suiyuan.iragent.rag.pipeline;

import com.suiyuan.iragent.rag.embedding.EmbeddingService;
import com.suiyuan.iragent.service.NoteChunkingService;
import com.suiyuan.iragent.service.NoteChunkingService.Chunk;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.collection.*;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.index.CreateIndexParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class NoteIngestionPipeline {

    private static final int EMBEDDING_DIM = 2048;

    private final EmbeddingService embeddingService;
    private final MilvusServiceClient milvusClient;
    private final NoteChunkingService chunkingService;
    private final JdbcTemplate jdbcTemplate;

    public NoteIngestionPipeline(EmbeddingService embeddingService,
                                  MilvusServiceClient milvusClient,
                                  NoteChunkingService chunkingService,
                                  JdbcTemplate jdbcTemplate) {
        this.embeddingService = embeddingService;
        this.milvusClient = milvusClient;
        this.chunkingService = chunkingService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public int ingest(String noteId, long userId, String title, String content,
                       String subject, String chapter, String tags, String fileType) {
        content = sanitizeContent(content);
        List<Chunk> chunks = chunkingService.chunk(content);
        if (chunks.isEmpty()) return 0;

        String collName = collectionName(userId);
        ensureCollection(collName);

        List<String> texts = chunks.stream().map(Chunk::content).toList();
        List<float[]> embeddings = embeddingService.embedBatch(texts);

        insertToMilvus(collName, texts, embeddings);

        jdbcTemplate.update(
                "INSERT INTO note (id, user_id, subject, chapter, title, content, tags, file_type, chunk_count) " +
                "VALUES (?,?,?,?,?,?,?,?,?) ON CONFLICT (id) DO UPDATE SET content=EXCLUDED.content, " +
                "subject=EXCLUDED.subject, chapter=EXCLUDED.chapter, tags=EXCLUDED.tags, " +
                "chunk_count=EXCLUDED.chunk_count, updated_at=NOW()",
                noteId, userId, subject, chapter, title, content, tags, fileType, chunks.size());

        jdbcTemplate.update("DELETE FROM note_chunk WHERE note_id = ?", noteId);
        for (int i = 0; i < chunks.size(); i++) {
            jdbcTemplate.update(
                    "INSERT INTO note_chunk (id, note_id, user_id, chunk_index, knowledge_point, content) " +
                    "VALUES (?,?,?,?,?,?)",
                    UUID.randomUUID().toString().substring(0, 16),
                    noteId, userId, i, truncate(chunks.get(i).knowledgePoint(), 250),
                    chunks.get(i).content());
        }

        log.info("笔记入库完成: noteId={}, userId={}, chunks={}", noteId, userId, chunks.size());
        return chunks.size();
    }

    private String sanitizeContent(String content) {
        if (content == null) return "";
        StringBuilder sb = new StringBuilder(content.length());
        for (char c : content.toCharArray()) {
            if (c == 0 || c == '﻿') continue;
            if (c < 32 && c != '\n' && c != '\r' && c != '\t') continue;
            sb.append(c);
        }
        return sb.toString().trim();
    }

    private String collectionName(long userId) {
        return "kb_notes_" + userId;
    }

    private void ensureCollection(String collName) {
        var hasResult = milvusClient.hasCollection(
                HasCollectionParam.newBuilder().withCollectionName(collName).build());
        if (hasResult.getData() != null && hasResult.getData()) return;

        List<FieldType> fields = List.of(
                FieldType.newBuilder().withName("id").withDataType(DataType.Int64)
                        .withPrimaryKey(true).withAutoID(true).build(),
                FieldType.newBuilder().withName("content").withDataType(DataType.VarChar)
                        .withMaxLength(65535).build(),
                FieldType.newBuilder().withName("embedding").withDataType(DataType.FloatVector)
                        .withDimension(EMBEDDING_DIM).build()
        );

        milvusClient.createCollection(
                CreateCollectionParam.newBuilder().withCollectionName(collName)
                        .withFieldTypes(fields).build());

        milvusClient.createIndex(
                CreateIndexParam.newBuilder().withCollectionName(collName)
                        .withFieldName("embedding").withIndexType(IndexType.FLAT)
                        .withMetricType(MetricType.L2).build());

        milvusClient.loadCollection(
                LoadCollectionParam.newBuilder().withCollectionName(collName).build());

        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        log.info("Per-user Milvus Collection 创建完成: {}", collName);
    }

    private void insertToMilvus(String collName, List<String> texts, List<float[]> embeddings) {
        for (int i = 0; i < Math.min(texts.size(), embeddings.size()); i++) {
            List<Float> vec = new ArrayList<>();
            for (float v : embeddings.get(i)) vec.add(v);

            List<InsertParam.Field> fields = List.of(
                    new InsertParam.Field("content", List.of(texts.get(i))),
                    new InsertParam.Field("embedding", List.of(vec))
            );

            milvusClient.insert(InsertParam.newBuilder()
                    .withCollectionName(collName).withFields(fields).build());
        }
        milvusClient.flush(io.milvus.param.collection.FlushParam.newBuilder()
                .withCollectionNames(List.of(collName)).build());
    }
    private String truncate(String s, int max) { return s != null && s.length() > max ? s.substring(0, max) : s; }
}
