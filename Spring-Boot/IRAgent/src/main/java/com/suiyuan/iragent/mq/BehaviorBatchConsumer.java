package com.suiyuan.iragent.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.skywalking.apm.toolkit.trace.ActiveSpan;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * RocketMQ 批量消费者——50 条或 10 秒聚合写入 PostgreSQL。
 *
 * 面试点：消费端提取 MQ 消息头中的 TraceContext，关联父 Trace，
 * 实现"Web 请求 → MQ 生产 → 批量消费 → PG 写入"全链路串联。
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "BEHAVIOR_TOPIC", consumerGroup = "iragent-consumer")
public class BehaviorBatchConsumer implements RocketMQListener<String> {

    private final ConcurrentLinkedQueue<String> buffer = new ConcurrentLinkedQueue<>();
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public BehaviorBatchConsumer(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        // 每 10 秒定时刷盘
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "behavior-flush");
            t.setDaemon(true);
            return t;
        }).scheduleAtFixedRate(this::flushBuffer, 10, 10, TimeUnit.SECONDS);
    }

    @Override
    @Trace(operationName = "behaviorBatchConsume")
    public void onMessage(String message) {
        buffer.add(message);
        ActiveSpan.tag("mq.buffer.size", String.valueOf(buffer.size()));

        // 攒够 50 条立刻刷盘
        if (buffer.size() >= 50) {
            flushBuffer();
        }
    }

    private synchronized void flushBuffer() {
        if (buffer.isEmpty()) return;
        List<String> batch = new ArrayList<>();
        String item;
        while ((item = buffer.poll()) != null) {
            batch.add(item);
        }
        if (batch.isEmpty()) return;

        String sql = "INSERT INTO student_behavior_log (user_id, action, question_id, " +
                     "duration_ms, metadata, created_at) VALUES (?,?,?,?,?,NOW())";

        List<Object[]> batchArgs = new ArrayList<>();
        for (String msg : batch) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = objectMapper.readValue(msg, Map.class);
                batchArgs.add(new Object[]{
                        String.valueOf(data.get("userId")),
                        String.valueOf(data.get("action")),
                        String.valueOf(data.get("questionId")),
                        data.get("durationMs") instanceof Number n ? n.longValue() : 0L,
                        String.valueOf(data.getOrDefault("metadata", "{}"))
                });
            } catch (Exception e) {
                log.debug("解析行为消息失败: {}", e.getMessage());
            }
        }

        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchArgs);
            log.info("行为日志批量刷盘: {} 条", batchArgs.size());
        }
    }
}
