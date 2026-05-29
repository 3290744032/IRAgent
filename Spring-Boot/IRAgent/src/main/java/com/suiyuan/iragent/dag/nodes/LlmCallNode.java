package com.suiyuan.iragent.dag.nodes;

import com.suiyuan.iragent.config.VolcEngineStreamingClient;
import com.suiyuan.iragent.dag.core.DagNode;
import com.suiyuan.iragent.dag.core.ExecutionContext;
import com.suiyuan.iragent.dag.core.NodeResult;
import com.suiyuan.iragent.dag.engine.DagExecutor.NodeHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * DAG LLM 流式节点——使用 VolcEngineStreamingClient 逐字接收大模型输出，
 * 每收到一个 chunk 立刻通过 onTextChunk 回调推送给 SSE 前端。
 */
@Slf4j
public class LlmCallNode implements NodeHandler {

    private final VolcEngineStreamingClient streamingClient;

    public LlmCallNode(VolcEngineStreamingClient streamingClient) {
        this.streamingClient = streamingClient;
    }

    @Override
    @SuppressWarnings("unchecked")
    public NodeResult handle(DagNode node, ExecutionContext context) {
        String prompt = (String) node.getConfig().get("prompt");
        String question = context.getVariable("question");
        String studentAnswer = context.getVariable("studentAnswer");

        // 逐字流式回调：（nodeId, textChunk）→ 推送到 SSE
        java.util.function.BiConsumer<String, String> onChunk =
                (java.util.function.BiConsumer<String, String>) context.getVariable("_onTextChunk");

        if (prompt == null || question == null) {
            return NodeResult.builder().nodeId(node.getId()).success(false)
                    .error("prompt 或 question 不能为空").build();
        }

        StringBuilder fullPrompt = new StringBuilder(prompt);
        fullPrompt.append("\n\n【题目】\n").append(question);
        if (studentAnswer != null && !studentAnswer.isBlank()) {
            fullPrompt.append("\n\n【学生的错误答案】\n").append(studentAnswer);
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> fullResponse = new AtomicReference<>("");
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        long start = System.currentTimeMillis();

        streamingClient.streamChat(
                fullPrompt.toString(),
                text -> {
                    fullResponse.updateAndGet(s -> s + text);
                    if (onChunk != null) {
                        onChunk.accept(node.getId(), text);
                    }
                },
                () -> latch.countDown(),
                error -> {
                    errorRef.set(error);
                    latch.countDown();
                });

        try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        long duration = System.currentTimeMillis() - start;

        if (errorRef.get() != null) {
            return NodeResult.builder().nodeId(node.getId()).success(false)
                    .error(errorRef.get().getMessage()).durationMs(duration).build();
        }

        String response = fullResponse.get();
        Map<String, Object> output = Map.of("content", response);
        int tokens = fullPrompt.length() / 4 + response.length() / 4;

        return NodeResult.builder()
                .nodeId(node.getId()).success(true)
                .output(output).durationMs(duration).tokensUsed(tokens).build();
    }
}
