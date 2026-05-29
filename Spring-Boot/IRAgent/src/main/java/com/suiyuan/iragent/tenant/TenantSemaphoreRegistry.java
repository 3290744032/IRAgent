package com.suiyuan.iragent.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 多租户算力隔离——JVM 内 Semaphore 信号量配额。
 *
 * 面试点：为什么不在网关层限流？因为限的是最贵资源（LLM 算力），不是 HTTP 请求。
 * 一个学校查 100 次历史记录（10ms Redis）和一个学校 5 并发 LLM 诊断（10s），成本完全不同。
 */
@Slf4j
@Component
public class TenantSemaphoreRegistry {

    private static final int DEFAULT_MAX_CONCURRENT = 5;
    private final Map<String, Semaphore> semaphores = new ConcurrentHashMap<>();

    public boolean tryAcquire(String tenantId, long timeoutMs) {
        Semaphore sem = semaphores.computeIfAbsent(tenantId,
                k -> new Semaphore(DEFAULT_MAX_CONCURRENT, true));
        try {
            boolean acquired = sem.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
            if (!acquired) {
                log.warn("租户 {} 信号量耗尽（最大={}），返回 429", tenantId,
                        sem.availablePermits() + sem.getQueueLength());
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void release(String tenantId) {
        Semaphore sem = semaphores.get(tenantId);
        if (sem != null) sem.release();
    }

    public void setQuota(String tenantId, int maxConcurrent) {
        Semaphore old = semaphores.get(tenantId);
        Semaphore newSem = new Semaphore(maxConcurrent, true);
        if (old != null) {
            newSem.drainPermits();
            int available = old.availablePermits();
            if (available > maxConcurrent) available = maxConcurrent;
            newSem.release(available);
        }
        semaphores.put(tenantId, newSem);
        log.info("租户 {} 配额已更新: maxConcurrent={}", tenantId, maxConcurrent);
    }

    public Map<String, Object> getQuotaInfo(String tenantId) {
        Semaphore sem = semaphores.get(tenantId);
        Map<String, Object> info = new ConcurrentHashMap<>();
        if (sem != null) {
            info.put("available", sem.availablePermits());
            info.put("queueLength", sem.getQueueLength());
        } else {
            info.put("available", DEFAULT_MAX_CONCURRENT);
            info.put("queueLength", 0);
        }
        return info;
    }

    public Map<String, Map<String, Object>> getAllQuotas() {
        Map<String, Map<String, Object>> result = new ConcurrentHashMap<>();
        semaphores.forEach((id, sem) -> {
            Map<String, Object> info = new ConcurrentHashMap<>();
            info.put("available", sem.availablePermits());
            info.put("queueLength", sem.getQueueLength());
            result.put(id, info);
        });
        return result;
    }
}
