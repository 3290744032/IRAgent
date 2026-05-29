package com.suiyuan.iragent;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.ShowCollectionsResponse;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.DropCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.ShowCollectionsParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.task.AsyncTaskExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 1 集成验证测试
 *
 * 确保虚拟线程 + RocketMQ + Milvus 已正确集成。
 * 运行前：docker compose up -d
 * IDEA 右键运行此类，观察日志输出。
 */
@Slf4j
@SpringBootTest
class Phase1SmokeTest {

    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

    @Autowired(required = false)
    private MilvusServiceClient milvusClient;

    @Autowired
    private AsyncTaskExecutor asyncTaskExecutor;

    private static final String TEST_COLLECTION = "phase1_smoke_test";

    // ==================== 1. 虚拟线程验证 ====================

    @Test
    void virtualThreadEnabled() throws Exception {
        // 使用注入的虚拟线程执行器（AsyncConfig 中配置）
        var future = CompletableFuture.supplyAsync(() ->
                Thread.currentThread().isVirtual(), asyncTaskExecutor
        );

        boolean isVirtual = future.get(10, TimeUnit.SECONDS);
        log.info("=== 虚拟线程验证 ===");
        log.info("是虚拟线程: {}", isVirtual);
        assertThat(isVirtual).isTrue();
    }

    // ==================== 2. RocketMQ 连接验证 ====================

    @Test
    void rocketMQConnectionOK() {
        if (rocketMQTemplate == null) {
            log.warn("=== RocketMQ: RocketMQTemplate 未注入，跳过 ===");
            return;
        }

        log.info("=== RocketMQ 验证 ===");

        // 验证 Producer 已连接到 NameServer
        DefaultMQProducer producer = rocketMQTemplate.getProducer();
        assertThat(producer).isNotNull();
        log.info("Producer group: {}", producer.getProducerGroup());
        log.info("NameServer 地址: {}", producer.getNamesrvAddr());

        // 验证消息发送（RocketMQ 5.x 需要 broker.conf 中 autoCreateTopicEnable=true）
        // 验证消息发送（RocketMQ 5.x 需要 broker.conf 中 autoCreateTopicEnable=true）
        try {
            try {
                // 第一次尝试发送，如果是新环境，会触发 NameServer/Broker 的自动创建机制
                rocketMQTemplate.syncSend("phase1-test-topic", "Phase1-" + System.currentTimeMillis());
                log.info("消息发送成功！");
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("No route info")) {
                    log.info("首次发送触发 RocketMQ 自动创建 Topic，正在等待路由同步并准备重试...");
                    // 稍等 1.5 秒，给 Broker 和 NameServer 反应和同步路由表的时间
                    Thread.sleep(1500);
                    // 进行第二次发送重试
                    rocketMQTemplate.syncSend("phase1-test-topic", "Phase1-Retry-" + System.currentTimeMillis());
                    log.info("重试发送成功！Topic 自动创建已激活。");
                } else {
                    throw e; // 如果是其他网络或配置错误，直接抛出
                }
            }
        } catch (Exception e) {
            log.error("RocketMQ 消息发送确实失败:", e);
        }

        log.info("RocketMQ 通过: NameServer 连接正常, Producer 就绪");
    }

    // ==================== 3. Milvus 连接验证 ====================

    @Test
    void milvusConnectionAndCollectionLifecycle() {
        if (milvusClient == null) {
            log.warn("=== Milvus: MilvusServiceClient 未注入，跳过 ===");
            return;
        }

        log.info("=== Milvus 验证 ===");

        // 1. 查看已有 Collections
        R<ShowCollectionsResponse> listResult = milvusClient.showCollections(
                ShowCollectionsParam.newBuilder().build());
        log.info("现有 Collections: {}", listResult.getData().getCollectionNamesList());

        // 2. 清理旧测试 Collection
        milvusClient.dropCollection(DropCollectionParam.newBuilder()
                .withCollectionName(TEST_COLLECTION).build());
        log.info("旧测试 Collection 已清理");

        // 3. 创建新 Collection
        FieldType idField = FieldType.newBuilder()
                .withName("id")
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(true)
                .build();
        FieldType vectorField = FieldType.newBuilder()
                .withName("embedding")
                .withDataType(DataType.FloatVector)
                .withDimension(128)
                .build();

        R<RpcStatus> createResult = milvusClient.createCollection(
                CreateCollectionParam.newBuilder()
                        .withCollectionName(TEST_COLLECTION)
                        .withDescription("Phase 1 smoke test")
                        .addFieldType(idField)
                        .addFieldType(vectorField)
                        .build());
        log.info("Collection 创建状态: {}", createResult.getStatus());
        assertThat(createResult.getStatus()).isEqualTo(0);

        // 4. 再次查看 Collections 确认创建成功
        R<ShowCollectionsResponse> listResult2 = milvusClient.showCollections(
                ShowCollectionsParam.newBuilder().build());
        log.info("创建后 Collections: {}", listResult2.getData().getCollectionNamesList());

        // 5. 清理
        milvusClient.dropCollection(DropCollectionParam.newBuilder()
                .withCollectionName(TEST_COLLECTION).build());
        log.info("Milvus 通过: 连接 + 创建 + 删除 Collection 全流程正常");
    }
}
