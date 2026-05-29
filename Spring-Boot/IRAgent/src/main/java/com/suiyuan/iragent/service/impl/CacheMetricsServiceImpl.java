package com.suiyuan.iragent.service.impl;

import com.suiyuan.iragent.service.CacheMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CacheMetricsServiceImpl implements CacheMetricsService {

    private final StringRedisTemplate redis;

    @Override
    public void recordHit(long responseTimeMs, int tokensSaved) {
        redis.opsForValue().increment("cache:metrics:hits");
        redis.opsForList().rightPush("cache:metrics:response_times", String.valueOf(responseTimeMs));
        redis.opsForValue().increment("cache:metrics:tokens_saved", tokensSaved);
    }

    @Override
    public void recordMiss(long responseTimeMs) {
        redis.opsForValue().increment("cache:metrics:misses");
        redis.opsForList().rightPush("cache:metrics:response_times", String.valueOf(responseTimeMs));
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        String hits = redis.opsForValue().get("cache:metrics:hits");
        String misses = redis.opsForValue().get("cache:metrics:misses");
        String tokensSaved = redis.opsForValue().get("cache:metrics:tokens_saved");
        stats.put("hits", hits != null ? Long.parseLong(hits) : 0);
        stats.put("misses", misses != null ? Long.parseLong(misses) : 0);
        stats.put("tokensSaved", tokensSaved != null ? Long.parseLong(tokensSaved) : 0);
        long total = (long) stats.get("hits") + (long) stats.get("misses");
        stats.put("hitRate", total > 0 ? (double) stats.get("hits") / total : 0.0);
        return stats;
    }
}
