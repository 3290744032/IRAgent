package com.suiyuan.iragent.controller;

import com.suiyuan.iragent.service.DailyPracticeService;
import com.suiyuan.iragent.utils.ApiResponse;
import com.suiyuan.iragent.utils.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v3/daily-practice")
@Tag(name = "每日一练", description = "每日一练——60%薄弱考点+40%高频随机，支持提交批改")
@SecurityRequirement(name = "TokenAuth")
public class DailyPracticeController {

    private final DailyPracticeService service;

    public DailyPracticeController(DailyPracticeService service) {
        this.service = service;
    }

    @Operation(summary = "获取每日练习题", description = "可指定知识点（逗号分隔）以生成定向练习")
    @GetMapping
    public ApiResponse<Map<String, Object>> getQuestions(
            @RequestParam(required = false) String subject,
            @RequestParam(defaultValue = "5") int count,
            @RequestParam(required = false) String knowledgePoints) {
        return ApiResponse.success(service.generatePractice(
                UserHolder.getUser().getUserId(), subject, count, knowledgePoints));
    }

    @Operation(summary = "提交答题结果")
    @PostMapping("/submit")
    public ApiResponse<Map<String, Object>> submit(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(service.submitAnswers(
                UserHolder.getUser().getUserId(), body));
    }
}
