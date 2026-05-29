package com.suiyuan.iragent.dag.engine;

import com.suiyuan.iragent.dag.core.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kahn 拓扑排序单元测试——覆盖 5 种图结构。
 */
class TopologicalSorterTest {

    // ====== 1. 单节点图 ======

    @Test
    void singleNode() {
        DagGraph graph = buildGraph("single",
                node("A", NodeType.LLM_CALL, List.of()));

        List<List<DagNode>> layers = TopologicalSorter.sort(graph);
        assertThat(layers).hasSize(1);
        assertThat(layers.get(0)).hasSize(1);
        assertThat(layers.get(0).get(0).getId()).isEqualTo("A");
    }

    // ====== 2. 链式图（全串行）======

    @Test
    void chainGraph() {
        DagGraph graph = buildGraph("chain",
                node("A", NodeType.LLM_CALL, List.of()),
                node("B", NodeType.TRANSFORM, List.of("A")),
                node("C", NodeType.AGGREGATE, List.of("B")));

        List<List<DagNode>> layers = TopologicalSorter.sort(graph);

        // 每层只有一个节点，串行执行
        assertThat(layers).hasSize(3);
        assertThat(layers.get(0)).extracting(DagNode::getId).containsExactly("A");
        assertThat(layers.get(1)).extracting(DagNode::getId).containsExactly("B");
        assertThat(layers.get(2)).extracting(DagNode::getId).containsExactly("C");
    }

    // ====== 3. 星形图（全并行）======

    @Test
    void starGraph() {
        DagGraph graph = buildGraph("star",
                node("A", NodeType.LLM_CALL, List.of()),
                node("B", NodeType.LLM_CALL, List.of()),
                node("C", NodeType.LLM_CALL, List.of()));

        List<List<DagNode>> layers = TopologicalSorter.sort(graph);

        // 全部在同一层，可并发执行
        assertThat(layers).hasSize(1);
        assertThat(layers.get(0)).hasSize(3);
    }

    // ====== 4. 标准 DAG ======

    @Test
    void standardDag() {
        DagGraph graph = buildGraph("dag",
                node("A", NodeType.LLM_CALL, List.of()),
                node("B", NodeType.LLM_CALL, List.of("A")),
                node("C", NodeType.LLM_CALL, List.of("A")),
                node("D", NodeType.AGGREGATE, List.of("B", "C")));

        List<List<DagNode>> layers = TopologicalSorter.sort(graph);

        // Layer1: A, Layer2: B+C(并行), Layer3: D
        assertThat(layers).hasSize(3);
        assertThat(layers.get(0)).extracting(DagNode::getId).containsExactly("A");
        assertThat(layers.get(1)).extracting(DagNode::getId).containsExactlyInAnyOrder("B", "C");
        assertThat(layers.get(2)).extracting(DagNode::getId).containsExactly("D");
    }

    // ====== 5. 含环图 ======

    @Test
    void cycleGraph() {
        DagGraph graph = buildGraph("cycle",
                node("A", NodeType.LLM_CALL, List.of("B")),
                node("B", NodeType.LLM_CALL, List.of("A")));

        assertThatThrownBy(() -> TopologicalSorter.sort(graph))
                .isInstanceOf(DagCycleException.class)
                .hasMessageContaining("环路");
    }

    // ====== 辅助方法 ======

    private static DagNode node(String id, NodeType type, List<String> dependsOn) {
        return DagNode.builder().id(id).type(type).dependsOn(dependsOn).build();
    }

    private static DagGraph buildGraph(String name, DagNode... nodes) {
        DagGraph graph = new DagGraph();
        graph.setName(name);
        graph.setNodes(List.of(nodes));
        graph.setEdges(List.of());
        return graph;
    }
}
