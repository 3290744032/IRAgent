package com.suiyuan.iragent.dag.core;

import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class ExecutionContext {
    /** 用户上下文 */
    private String userId;
    /** 全局变量 */
    private Map<String, Object> variables = new ConcurrentHashMap<>();
    /** 各节点输出快照 */
    private Map<String, NodeResult> nodeOutputs = new ConcurrentHashMap<>();

    public ExecutionContext(String userId) {
        this.userId = userId;
    }

    public void putVariable(String key, Object value) {
        variables.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getVariable(String key) {
        return (T) variables.get(key);
    }

    public void putNodeResult(NodeResult result) {
        nodeOutputs.put(result.getNodeId(), result);
    }

    public NodeResult getNodeResult(String nodeId) {
        return nodeOutputs.get(nodeId);
    }

    /** 获取上一个节点的输出，供下游节点使用 */
    public String getPreviousOutput(String nodeId) {
        NodeResult result = nodeOutputs.get(nodeId);
        if (result == null || result.getOutput() == null) return null;
        Object content = result.getOutput().get("content");
        return content != null ? content.toString() : null;
    }
}
