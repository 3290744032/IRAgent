package com.suiyuan.iragent.service;

import java.util.Map;

public interface CacheMetricsService {

    void recordHit(long responseTimeMs, int tokensSaved);

    void recordMiss(long responseTimeMs);

    Map<String, Object> getStats();
}
