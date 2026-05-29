package com.suiyuan.iragent.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.skywalking.apm.toolkit.trace.ActiveSpan;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * RocketMQ 行为消息生产者。
 *
 * 学生刷题行为（答题、看视频、错题）异步打入 MQ，非阻塞。
 * 面试点：跨进程 TraceContext 注入——通过 SkyWalking ContextCarrier 将 TraceID 写入 MQ 消息头。
 */
@Slf4j
@Component
public class BehaviorMessageProducer {

    private static final String BEHAVIOR_TOPIC = "BEHAVIOR_TOPIC";

    private final RocketMQTemplate rocketMQTemplate;

    public BehaviorMessageProducer(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    @Trace(operationName = "behaviorProduce")
    public void sendBehavior(String userId, String action, String questionId,
                              long durationMs, Map<String, Object> metadata) {

        // 注入 SkyWalking TraceContext 到 MQ 消息头
        String traceId = TraceContext.traceId();
        String sw8Header = traceId != null ? traceId : "unknown";

        Message<String> message = MessageBuilder
                .withPayload(buildPayload(userId, action, questionId, durationMs, metadata))
                .setHeader("sw8", sw8Header)
                .setHeader("userId", userId)
                .setHeader("action", action)
                .build();

        try {
            rocketMQTemplate.asyncSend(BEHAVIOR_TOPIC, message, null);
            ActiveSpan.tag("mq.topic", BEHAVIOR_TOPIC);
            ActiveSpan.tag("mq.action", action);
            log.debug("行为消息已发送: userId={}, action={}, traceId={}", userId, action, sw8Header);
        } catch (Exception e) {
            log.error("行为消息发送失败: userId={}, action={}", userId, action, e);
            ActiveSpan.error(e);
        }
    }

    private String buildPayload(String userId, String action, String questionId,
                                 long durationMs, Map<String, Object> metadata) {
        return String.format("{\"userId\":\"%s\",\"action\":\"%s\",\"questionId\":\"%s\"," +
                "\"durationMs\":%d,\"timestamp\":%d,\"metadata\":%s}",
                userId, action, questionId, durationMs, System.currentTimeMillis(),
                metadata != null ? metadata.toString() : "{}");
    }
}
