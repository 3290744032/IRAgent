package com.suiyuan.iragent.dag.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DagGraph {
    private String name;
    private List<DagNode> nodes;
    private List<DagEdge> edges;

    @JsonIgnore
    private Map<String, DagNode> nodeIndex;

    @JsonIgnore
    private Map<String, Set<String>> adjacencyList;

    /**
     * 校验 DAG 是否合法：无环路、无孤立节点
     */
    public void validate() {
        buildIndex();
        detectCycle();
        detectOrphanNodes();
    }

    private void buildIndex() {
        nodeIndex = new HashMap<>();
        adjacencyList = new HashMap<>();
        for (DagNode node : nodes) {
            nodeIndex.put(node.getId(), node);
            adjacencyList.put(node.getId(), new HashSet<>());
        }
        for (DagNode node : nodes) {
            if (node.getDependsOn() != null) {
                for (String dep : node.getDependsOn()) {
                    adjacencyList.computeIfAbsent(dep, k -> new HashSet<>()).add(node.getId());
                }
            }
        }
    }

    private void detectCycle() {
        Map<String, Integer> inDegree = new HashMap<>();
        for (String id : nodeIndex.keySet()) {
            inDegree.put(id, 0);
        }
        for (DagNode node : nodes) {
            if (node.getDependsOn() != null) {
                for (String dep : node.getDependsOn()) {
                    inDegree.merge(node.getId(), 1, Integer::sum);
                }
            }
        }
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> e : inDegree.entrySet()) {
            if (e.getValue() == 0) queue.add(e.getKey());
        }
        int visited = 0;
        while (!queue.isEmpty()) {
            String id = queue.poll();
            visited++;
            for (String neighbor : adjacencyList.getOrDefault(id, Set.of())) {
                inDegree.merge(neighbor, -1, Integer::sum);
                if (inDegree.get(neighbor) == 0) queue.add(neighbor);
            }
        }
        if (visited != nodes.size()) {
            throw new DagCycleException("DAG 图中存在环路，已完成拓扑排序节点数: " + visited + "/" + nodes.size());
        }
    }

    private void detectOrphanNodes() {
        Set<String> referenced = new HashSet<>();
        for (DagNode node : nodes) {
            if (node.getDependsOn() != null) referenced.addAll(node.getDependsOn());
            referenced.add(node.getId());
        }
        for (String id : adjacencyList.keySet()) {
            if (!referenced.contains(id)) {
                throw new IllegalArgumentException("发现孤立节点: " + id);
            }
        }
    }

    public DagNode getNode(String id) {
        if (nodeIndex == null) buildIndex();
        return nodeIndex.get(id);
    }
}
