package com.suiyuan.iragent.rag.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suiyuan.iragent.rag.embedding.EmbeddingService;
import com.suiyuan.iragent.service.CacheMetricsService;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.MetricType;
import io.milvus.param.dml.SearchParam;
import io.milvus.response.SearchResultsWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
public class SemanticCacheService {

    private static final long CACHE_TTL_DAYS = 7;

    private final EmbeddingService embeddingService;
    private final MilvusServiceClient milvusClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheMetricsService metricsService;

    public SemanticCacheService(EmbeddingService embeddingService,
                                 MilvusServiceClient milvusClient,
                                 StringRedisTemplate redisTemplate,
                                 ObjectMapper objectMapper,
                                 CacheMetricsService metricsService) {
        this.embeddingService = embeddingService;
        this.milvusClient = milvusClient;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.metricsService = metricsService;
    }

    public String getOrCompute(String question, Supplier<String> fallbackFn) {
        long startMs = System.currentTimeMillis();
        String cacheKey = "semcache:" + md5(question);

        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            long elapsed = System.currentTimeMillis() - startMs;
            log.info("语义缓存命中(Redis): key={}, elapsed={}ms", cacheKey.substring(0, 16), elapsed);
            metricsService.recordHit(elapsed, estimateTokens(cached));
            return cached;
        }

        log.info("语义缓存未命中: question_md5={}", md5(question).substring(0, 8));
        long llmStart = System.currentTimeMillis();
        String result = fallbackFn.get();
        long llmElapsed = System.currentTimeMillis() - llmStart;

        redisTemplate.opsForValue().set(cacheKey, result, CACHE_TTL_DAYS, TimeUnit.DAYS);
        metricsService.recordMiss(llmElapsed);

        return result;
    }

    private int estimateTokens(String cached) {
        return cached != null ? cached.length() / 4 : 0;
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
