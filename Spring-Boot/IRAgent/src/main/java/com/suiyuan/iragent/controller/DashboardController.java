package com.suiyuan.iragent.controller;

import com.suiyuan.iragent.service.DashboardService;
import com.suiyuan.iragent.utils.ApiResponse;
import com.suiyuan.iragent.utils.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v3/dashboard")
@Tag(name = "备考仪表盘", description = "备考进度/学习周报/掌握度雷达/今日任务")
@SecurityRequirement(name = "TokenAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "备考概览")
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.success(dashboardService.getOverview(
                UserHolder.getUser().getUserId()));
    }

    @Operation(summary = "学习周报")
    @GetMapping("/weekly-report")
    public ApiResponse<Map<String, Object>> weeklyReport() {
        return ApiResponse.success(dashboardService.getWeeklyReport(
                UserHolder.getUser().getUserId()));
    }

    @Operation(summary = "掌握度雷达图")
    @GetMapping("/mastery-radar")
    public ApiResponse<Map<String, Object>> masteryRadar() {
        return ApiResponse.success(dashboardService.getMasteryRadar(
                UserHolder.getUser().getUserId()));
    }

    @Operation(summary = "今日任务")
    @GetMapping("/today-tasks")
    public ApiResponse<List<Map<String, Object>>> todayTasks() {
        return ApiResponse.success(dashboardService.getTodayTasks(
                UserHolder.getUser().getUserId()));
    }
}
