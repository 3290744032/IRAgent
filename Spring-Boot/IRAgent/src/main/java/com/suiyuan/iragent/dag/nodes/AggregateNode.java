package com.suiyuan.iragent.dag.nodes;

import com.suiyuan.iragent.dag.core.DagNode;
import com.suiyuan.iragent.dag.core.ExecutionContext;
import com.suiyuan.iragent.dag.core.NodeResult;
import com.suiyuan.iragent.dag.engine.DagExecutor.NodeHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * 聚合节点——将并行节点的诊断结果汇总为结构化 JSON。
 */
public class AggregateNode implements NodeHandler {

    @Override
    @SuppressWarnings("unchecked")
    public NodeResult handle(DagNode node, ExecutionContext context) {
        Map<String, Object> config = node.getConfig();
        java.util.List<String> sources = config != null
                ? (java.util.List<String>) config.get("sourceNodes")
                : null;

        if (sources == null || sources.isEmpty()) {
            return NodeResult.builder()
                    .nodeId(node.getId()).success(false)
                    .error("AggregateNode 缺少 sourceNodes 配置").build();
        }

        Map<String, Object> aggregated = new HashMap<>();
        for (String srcId : sources) {
            NodeResult result = context.getNodeResult(srcId);
            if (result != null && result.isSuccess()) {
                aggregated.put(srcId, result.getOutput());
            } else {
                aggregated.put(srcId, Map.of("error", result != null ? result.getError() : "unknown"));
            }
        }

        Map<String, Object> output = new HashMap<>();
        output.put("diagnosis", aggregated);
        output.put("summary", buildSummary(aggregated));

        return NodeResult.builder()
                .nodeId(node.getId()).success(true)
                .output(output).durationMs(0).build();
    }

    private String buildSummary(Map<String, Object> aggregated) {
        StringBuilder sb = new StringBuilder("错题诊断结果：\n");
        for (Map.Entry<String, Object> entry : aggregated.entrySet()) {
            sb.append("【").append(entry.getKey()).append("】\n");
            Object val = entry.getValue();
            if (val instanceof Map<?, ?> m) {
                Object content = m.get("content");
                if (content instanceof String s && s.length() > 200) {
                    sb.append(s, 0, 200).append("...\n");
                } else {
                    sb.append(content).append("\n");
                }
            }
        }
        return sb.toString();
    }
}
