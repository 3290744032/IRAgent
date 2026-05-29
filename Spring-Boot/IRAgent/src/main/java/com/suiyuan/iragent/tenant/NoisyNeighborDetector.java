package com.suiyuan.iragent.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Noisy Neighbor 吵闹邻居检测器。
 *
 * 监控每租户 Semaphore 等待队列，当某租户平均等待 > 5 秒时触发告警。
 * 面试点：结合 SkyWalking Profile 虚拟线程堆栈采样，精准定位抢占资源的租户。
 */
@Slf4j
@Component
public class NoisyNeighborDetector {

    private static final long ALERT_THRESHOLD_MS = 5000;

    private final TenantSemaphoreRegistry registry;
    private final Map<String, Long> lastAlertTime = new ConcurrentHashMap<>();

    public NoisyNeighborDetector(TenantSemaphoreRegistry registry) {
        this.registry = registry;
    }

    @Scheduled(fixedRate = 15000)
    public void checkNoisyNeighbors() {
        Map<String, Map<String, Object>> quotas = registry.getAllQuotas();

        for (Map.Entry<String, Map<String, Object>> entry : quotas.entrySet()) {
            String tenantId = entry.getKey();
            Map<String, Object> info = entry.getValue();

            int queueLength = ((Number) info.getOrDefault("queueLength", 0)).intValue();
            int available = ((Number) info.getOrDefault("available", 0)).intValue();

            // 队列堆积 + 无可用信号量 = 潜在吵闹邻居
            if (queueLength > 0 && available == 0) {
                long now = System.currentTimeMillis();
                long lastAlert = lastAlertTime.getOrDefault(tenantId, 0L);

                if (now - lastAlert > 30000) {  // 30s 内不重复告警
                    log.warn("吵闹邻居告警: tenant={}, queueLength={}, available={}. " +
                             "建议通过 SkyWalking Profile 对该租户虚拟线程堆栈采样排查。",
                            tenantId, queueLength, available);
                    lastAlertTime.put(tenantId, now);
                }
            }
        }
    }

    public String diagnose() {
        StringBuilder sb = new StringBuilder("Noisy Neighbor 诊断报告:\n");
        Map<String, Map<String, Object>> quotas = registry.getAllQuotas();
        for (Map.Entry<String, Map<String, Object>> entry : quotas.entrySet()) {
            sb.append(String.format("  租户 %s: available=%s, queueLength=%s\n",
                    entry.getKey(),
                    entry.getValue().get("available"),
                    entry.getValue().get("queueLength")));
        }
        return sb.toString();
    }
}
