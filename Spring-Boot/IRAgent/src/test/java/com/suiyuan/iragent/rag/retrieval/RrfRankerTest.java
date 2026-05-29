package com.suiyuan.iragent.rag.retrieval;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RRF 倒数排名融合算法单元测试。
 */
class RrfRankerTest {

    private final RrfRanker ranker = new RrfRanker();

    @Test
    void identicalListsProduceSameOrder() {
        // 两个完全相同的结果集，RRF 输出顺序不变
        var v1 = result("1", "text1", 0.9, List.of("导数"));
        var v2 = result("2", "text2", 0.8, List.of("极值"));
        var f1 = result("1", "text1", 0.7, List.of("导数"));
        var f2 = result("2", "text2", 0.6, List.of("极值"));

        List<RrfRanker.RankedResult> merged = ranker.merge(List.of(v1, v2), List.of(f1, f2), 5);

        assertThat(merged).hasSize(2);
        assertThat(merged.get(0).id()).isEqualTo("1");
        assertThat(merged.get(1).id()).isEqualTo("2");
    }

    @Test
    void crossSourceComplement() {
        // 向量路召回 [A,B,C]，全文路召回 [B,C,D]，RRF 应产生交叉互补
        var vA = result("A", "textA", 0.9, List.of("导数"));
        var vB = result("B", "textB", 0.8, List.of("极值"));
        var vC = result("C", "textC", 0.7, List.of("函数"));
        var fB = result("B", "textB", 0.9, List.of("极值"));
        var fC = result("C", "textC", 0.8, List.of("函数"));
        var fD = result("D", "textD", 0.7, List.of("积分"));

        List<RrfRanker.RankedResult> merged = ranker.merge(
                List.of(vA, vB, vC), List.of(fB, fC, fD), 5);

        // D 应该出现在结果中（全文路独有的）
        assertThat(merged.stream().anyMatch(r -> r.id().equals("D"))).isTrue();
        // A 也应该出现（向量路独有的）
        assertThat(merged.stream().anyMatch(r -> r.id().equals("A"))).isTrue();
        // B 在两路都高排名，应该排第一
        assertThat(merged.get(0).id()).isEqualTo("B");
    }

    @Test
    void weightedMerge() {
        // 向量路权重 0.8，全文路权重 0.2
        var vA = result("A", "textA", 1.0, List.of("导数"));
        var fB = result("B", "textB", 1.0, List.of("极值"));

        List<RrfRanker.RankedResult> merged = ranker.merge(
                List.of(vA), List.of(fB), 5, 0.8, 0.2);

        // A 应该在 B 前面，因为向量路权重高
        assertThat(merged.get(0).id()).isEqualTo("A");
    }

    private static FulltextSearchService.SearchResult result(String id, String text, double score, List<String> tags) {
        return new FulltextSearchService.SearchResult(id, text, score, "test", tags, "test", 2024);
    }
}
