package com.suiyuan.iragent.dag.engine;

import com.suiyuan.iragent.dag.core.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * DAG 执行引擎——虚拟线程按层并发调度。
 *
 * 支持进度回调——每个节点完成时立刻通知调用方，
 * 使得 SSE 流式接口可以实时推送执行状态。
 */
@Slf4j
public class DagExecutor {

    private final Map<String, NodeHandler> handlers;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public DagExecutor(Map<String, NodeHandler> handlers) {
        this.handlers = handlers;
    }

    /**
     * 执行 DAG 图，每个节点完成时通过 onNodeComplete 回调通知。
     */
    public Map<String, NodeResult> execute(DagGraph graph, ExecutionContext context,
                                            Consumer<NodeResult> onNodeComplete) {
        List<List<DagNode>> layers = TopologicalSorter.sort(graph);

        for (int layerIdx = 0; layerIdx < layers.size(); layerIdx++) {
            List<DagNode> layer = layers.get(layerIdx);
            log.info("DAG 执行层 {}/{}: {} 个节点并发", layerIdx + 1, layers.size(), layer.size());

            List<CompletableFuture<NodeResult>> futures = new ArrayList<>();

            for (DagNode node : layer) {
                NodeHandler handler = handlers.get(node.getType().name());
                if (handler == null) {
                    NodeResult errorResult = NodeResult.builder()
                            .nodeId(node.getId()).success(false)
                            .error("未找到处理器: " + node.getType()).build();
                    context.putNodeResult(errorResult);
                    onNodeComplete.accept(errorResult);
                    continue;
                }

                CompletableFuture<NodeResult> future = CompletableFuture
                        .supplyAsync(() -> {
                            long start = System.currentTimeMillis();
                            try {
                                log.info("节点开始: id={}, type={}", node.getId(), node.getType());
                                NodeResult result = handler.handle(node, context);
                                result.setDurationMs(System.currentTimeMillis() - start);
                                context.putNodeResult(result);
                                log.info("节点完成: id={}, 耗时={}ms", node.getId(), result.getDurationMs());
                                onNodeComplete.accept(result);
                                return result;
                            } catch (Exception e) {
                                log.error("节点异常: id={}", node.getId(), e);
                                NodeResult errorResult = NodeResult.builder()
                                        .nodeId(node.getId()).success(false)
                                        .error(e.getMessage())
                                        .durationMs(System.currentTimeMillis() - start).build();
                                context.putNodeResult(errorResult);
                                onNodeComplete.accept(errorResult);
                                return errorResult;
                            }
                        }, executor)
                        .orTimeout(180, TimeUnit.SECONDS);

                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }

        return context.getNodeOutputs();
    }

    @FunctionalInterface
    public interface NodeHandler {
        NodeResult handle(DagNode node, ExecutionContext context);
    }
}
