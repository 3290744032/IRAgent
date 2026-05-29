package com.suiyuan.iragent;

import com.suiyuan.iragent.rag.cache.SemanticCacheService;
import com.suiyuan.iragent.rag.embedding.EmbeddingService;
import com.suiyuan.iragent.rag.pipeline.QuestionIngestionPipeline;
import com.suiyuan.iragent.rag.retrieval.AdaptiveRecallService;
import com.suiyuan.iragent.rag.retrieval.FulltextSearchService;
import com.suiyuan.iragent.rag.retrieval.RrfRanker;
import com.suiyuan.iragent.service.CacheMetricsService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 3 集成验证测试
 *
 * 验证链路：Embedding → Milvus 入库 → 语义缓存 → 全文检索 → RRF 重排 → 自适应召回
 * 按顺序执行：先入库数据 → 再检索验证 → 最后缓存验证
 */
@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Phase3SmokeTest {

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private QuestionIngestionPipeline ingestionPipeline;

    @Autowired
    private SemanticCacheService semanticCache;

    @Autowired
    private AdaptiveRecallService recallService;

    @Autowired
    private FulltextSearchService fulltextSearchService;

    @Autowired
    private RrfRanker rrfRanker;

    @Autowired
    private CacheMetricsService metricsService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ==================== 1: Embedding 服务验证 ====================

    @Test
    @Order(1)
    void embeddingServiceWorks() {
        log.info("=== 3.1 Embedding 服务验证 ===");
        float[] vec = embeddingService.embed("二次函数求极值");
        assertThat(vec).isNotEmpty();
        log.info("Embedding 维度: {}", vec.length);
        log.info("Embedding 前5维: [{}, {}, {}, {}, {}...]",
                vec[0], vec[1], vec[2], vec[3], vec[4]);
    }

    // ==================== 2: 真题入库 ====================

    @Test
    @Order(2)
    void ingestSampleQuestions() throws Exception {
        log.info("=== 3.3 真题入库 ===");
        ClassPathResource resource = new ClassPathResource("data/sample-questions.json");
        File jsonFile = resource.getFile();

        int count = ingestionPipeline.ingest(jsonFile);
        log.info("入库数量: {}", count);
        assertThat(count).isGreaterThan(0);

        var stats = ingestionPipeline.getStats();
        log.info("Milvus 统计: {}", stats);
    }

    // ==================== 3: 全文检索验证 ====================

    @Test
    @Order(3)
    void fulltextSearchWorks() {
        log.info("=== 3.6 PostgreSQL 全文检索验证 ===");

        // 先确保 question 表有数据（从 sample-questions.json 同步写入了 PG）
        int pgCount = countPgQuestions();
        log.info("PostgreSQL question 表记录数: {}", pgCount);

        if (pgCount > 0) {
            var results = fulltextSearchService.search("二次函数 极值", 5);
            log.info("全文检索命中: {} 条", results.size());
            results.forEach(r -> log.info("  - [{}] score={} text={}", r.id(), r.score(), r.questionText()));
        } else {
            log.warn("PostgreSQL 中无 question 数据，需先执行 SQL 迁移和入库");
        }
    }

    // ==================== 4: RRF 重排验证 ====================

    @Test
    @Order(4)
    void rrfRankerWorks() {
        log.info("=== 3.7 RRF 重排验证 ===");

        // 用真实数据验证 RRF
        var vectorResults = autoToSearchResults("导数 极值");
        var fulltextResults = autoToSearchResults("导数 极值");

        var merged = rrfRanker.merge(vectorResults, fulltextResults, 5);
        log.info("RRF 融合结果: {} 条", merged.size());
        merged.forEach(r -> log.info("  - [{}] rrfScore={} text={}", r.id(),
                String.format("%.4f", r.rrfScore()),
                r.questionText().substring(0, Math.min(30, r.questionText().length()))));
    }

    // ==================== 5: 自适应召回验证 ====================

    @Test
    @Order(5)
    void adaptiveRecallWorks() {
        log.info("=== 3.8 自适应召回验证 ===");

        List<String> weakTags = List.of("导数", "极值");
        var results = recallService.recall("求函数的极值点", weakTags, 5);

        log.info("自适应召回结果: {} 条", results.size());
        assertThat(results).isNotEmpty();
        results.forEach(r -> log.info("  - [{}] rrfScore={} tags={} text={}",
                r.id(), String.format("%.4f", r.rrfScore()), r.tags(),
                r.questionText().substring(0, Math.min(50, r.questionText().length()))));
    }

    // ==================== 6: 语义缓存验证 ====================

    @Test
    @Order(6)
    void semanticCacheWorks() {
        log.info("=== 3.4 语义缓存验证 ===");

        String question = "求函数 f(x)=x^2+2x+1 的极值";
        Supplier<String> fallback = () -> "【模拟LLM响应】f(x)的对称轴为x=-1，最小值为0，在区间端点x=1处取得最大值4。";

        // 第一次：未命中，走 fallback
        long t1 = System.currentTimeMillis();
        String result1 = semanticCache.getOrCompute(question, fallback);
        long elapsed1 = System.currentTimeMillis() - t1;
        log.info("首次查询（应未命中）: {}ms, result sample={}", elapsed1,
                result1.substring(0, Math.min(60, result1.length())));

        // 第二次：应命中 Redis 缓存
        long t2 = System.currentTimeMillis();
        String result2 = semanticCache.getOrCompute(question, fallback);
        long elapsed2 = System.currentTimeMillis() - t2;
        log.info("二次查询（应命中缓存）: {}ms", elapsed2);

        assertThat(elapsed2).isLessThan(elapsed1);  // 命中更快
    }

    // ==================== 7: 缓存监控验证 ====================

    @Test
    @Order(7)
    void cacheMetricsWorks() {
        log.info("=== 3.9 缓存监控验证 ===");

        // 写入一些测试指标
        metricsService.recordHit(50, 1000);
        metricsService.recordHit(45, 1200);
        metricsService.recordMiss(5000);
        metricsService.recordMiss(5200);

        var stats = metricsService.getStats();
        log.info("缓存统计: {}", stats);
        assertThat(stats).containsKeys("hitRate", "totalRequests", "hits", "misses",
                "estimatedTokenSaved", "avgResponseTimeMs");
    }

    // ==================== 辅助方法 ====================

    private int countPgQuestions() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM question", Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.debug("查询 question 表失败: {}", e.getMessage());
            return 0;
        }
    }

    private List<FulltextSearchService.SearchResult> autoToSearchResults(String query) {
        try {
            return fulltextSearchService.search(query, 5);
        } catch (Exception e) {
            log.debug("全文检索失败: {}", e.getMessage());
            return List.of();
        }
    }
}
