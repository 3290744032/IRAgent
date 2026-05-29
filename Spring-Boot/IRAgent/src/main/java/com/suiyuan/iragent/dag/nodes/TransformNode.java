package com.suiyuan.iragent.dag.nodes;

import com.suiyuan.iragent.dag.core.DagNode;
import com.suiyuan.iragent.dag.core.ExecutionContext;
import com.suiyuan.iragent.dag.core.NodeResult;
import com.suiyuan.iragent.dag.engine.DagExecutor.NodeHandler;

import java.util.Map;

/**
 * 数据转换节点——将上游输出转为下游需要的格式。
 * 通过 config 中的 template 定义转换规则。
 */
public class TransformNode implements NodeHandler {

    @Override
    @SuppressWarnings("unchecked")
    public NodeResult handle(DagNode node, ExecutionContext context) {
        Map<String, Object> config = node.getConfig();
        if (config == null || !config.containsKey("sourceNodes")) {
            return NodeResult.builder()
                    .nodeId(node.getId()).success(false)
                    .error("TransformNode 缺少 sourceNodes 配置").build();
        }

        java.util.List<String> sources = (java.util.List<String>) config.get("sourceNodes");
        StringBuilder merged = new StringBuilder();

        for (String srcId : sources) {
            String output = context.getPreviousOutput(srcId);
            if (output != null) {
                merged.append("[").append(srcId).append("]\n").append(output).append("\n\n");
            }
        }

        Map<String, Object> output = new java.util.HashMap<>();
        output.put("content", merged.toString());
        output.put("sourceCount", sources.size());

        return NodeResult.builder()
                .nodeId(node.getId()).success(true)
                .output(output).durationMs(0).build();
    }
}
