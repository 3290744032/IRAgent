package com.suiyuan.iragent.controller;

import com.suiyuan.iragent.service.CacheMetricsService;
import com.suiyuan.iragent.utils.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/cache")
@Tag(name = "缓存监控", description = "语义缓存命中率、Token 节省量等指标")
public class CacheStatsController {

    private final CacheMetricsService metricsService;

    public CacheStatsController(CacheMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Operation(summary = "缓存统计")
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        return ApiResponse.success(metricsService.getStats());
    }
}
