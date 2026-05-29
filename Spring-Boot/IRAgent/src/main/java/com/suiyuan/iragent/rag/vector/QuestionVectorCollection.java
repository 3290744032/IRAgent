package com.suiyuan.iragent.rag.vector;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.collection.*;
import io.milvus.param.index.CreateIndexParam;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class QuestionVectorCollection {

    public static final String COLLECTION_NAME = "exam_questions";
    public static final int EMBEDDING_DIM = 2048;

    private final MilvusServiceClient milvusClient;

    public QuestionVectorCollection(MilvusServiceClient milvusClient) {
        this.milvusClient = milvusClient;
    }

    @PostConstruct
    public void init() {
        if (exists()) {
            // 维度变更时强制重建
            log.info("Milvus Collection 已存在，删除后重建以匹配维度 {}", EMBEDDING_DIM);
            milvusClient.dropCollection(
                    DropCollectionParam.newBuilder().withCollectionName(COLLECTION_NAME).build());
        }
        createCollection();
        createIndex();
        loadCollection();
        log.info("Milvus 真题向量 Collection 初始化完成: name={}, dim={}", COLLECTION_NAME, EMBEDDING_DIM);
    }

    public boolean exists() {
        var result = milvusClient.hasCollection(
                HasCollectionParam.newBuilder().withCollectionName(COLLECTION_NAME).build());
        return result.getData() != null && result.getData();
    }

    private void createCollection() {
        List<FieldType> fields = List.of(
                FieldType.newBuilder().withName("id").withDataType(DataType.Int64)
                        .withPrimaryKey(true).withAutoID(true).build(),
                FieldType.newBuilder().withName("question_text").withDataType(DataType.VarChar)
                        .withMaxLength(65535).build(),
                FieldType.newBuilder().withName("embedding").withDataType(DataType.FloatVector)
                        .withDimension(EMBEDDING_DIM).build(),
                FieldType.newBuilder().withName("tags").withDataType(DataType.VarChar)
                        .withMaxLength(1024).build(),
                FieldType.newBuilder().withName("province").withDataType(DataType.VarChar)
                        .withMaxLength(32).build(),
                FieldType.newBuilder().withName("year").withDataType(DataType.Int32).build()
        );

        var param = CreateCollectionParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withDescription("高考真题向量库")
                .withFieldTypes(fields)
                .build();

        var result = milvusClient.createCollection(param);
        if (result.getStatus() != 0) {
            throw new RuntimeException("Milvus Collection 创建失败: " + result.getMessage());
        }
        log.info("Milvus Collection 已创建: {}", COLLECTION_NAME);
    }

    private void createIndex() {
        milvusClient.createIndex(
                CreateIndexParam.newBuilder()
                        .withCollectionName(COLLECTION_NAME)
                        .withFieldName("embedding")
                        .withIndexType(IndexType.HNSW)
                        .withMetricType(MetricType.L2)
                        .withExtraParam("{\"M\": 16, \"efConstruction\": 200}")
                        .build());
        log.info("Milvus HNSW 索引创建完成");
    }

    private void loadCollection() {
        milvusClient.loadCollection(
                LoadCollectionParam.newBuilder().withCollectionName(COLLECTION_NAME).build());
        // 等待加载完成
        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        log.info("Milvus Collection 已加载到内存");
    }
}
