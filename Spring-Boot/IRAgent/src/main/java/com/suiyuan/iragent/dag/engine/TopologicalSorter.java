package com.suiyuan.iragent.dag.engine;

import com.suiyuan.iragent.dag.core.DagGraph;
import com.suiyuan.iragent.dag.core.DagNode;

import java.util.*;

/**
 * Kahn 算法拓扑排序，输出按层分组的可并行节点集合。
 * 同一层的节点互不依赖，可并发执行。
 */
public class TopologicalSorter {

    /**
     * @return List of layers, each layer contains nodes that can run in parallel
     */
    public static List<List<DagNode>> sort(DagGraph graph) {
        graph.validate();

        Map<String, DagNode> nodeMap = new LinkedHashMap<>();
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        Map<String, List<String>> children = new HashMap<>();

        for (DagNode node : graph.getNodes()) {
            nodeMap.put(node.getId(), node);
            inDegree.put(node.getId(), 0);
            children.put(node.getId(), new ArrayList<>());
        }

        for (DagNode node : graph.getNodes()) {
            if (node.getDependsOn() != null) {
                for (String dep : node.getDependsOn()) {
                    inDegree.merge(node.getId(), 1, Integer::sum);
                    children.computeIfAbsent(dep, k -> new ArrayList<>()).add(node.getId());
                }
            }
        }

        // BFS 按层分组
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> e : inDegree.entrySet()) {
            if (e.getValue() == 0) queue.add(e.getKey());
        }

        List<List<DagNode>> layers = new ArrayList<>();
        while (!queue.isEmpty()) {
            int layerSize = queue.size();
            List<DagNode> layer = new ArrayList<>();
            for (int i = 0; i < layerSize; i++) {
                String id = queue.poll();
                layer.add(nodeMap.get(id));
                for (String child : children.getOrDefault(id, List.of())) {
                    inDegree.merge(child, -1, Integer::sum);
                    if (inDegree.get(child) == 0) queue.add(child);
                }
            }
            layers.add(layer);
        }
        return layers;
    }
}
