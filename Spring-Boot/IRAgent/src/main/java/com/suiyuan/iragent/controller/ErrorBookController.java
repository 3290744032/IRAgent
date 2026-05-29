package com.suiyuan.iragent.controller;

import com.suiyuan.iragent.service.ErrorBookService;
import com.suiyuan.iragent.utils.ApiResponse;
import com.suiyuan.iragent.utils.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v3/errors")
@Tag(name = "错题本", description = "错题列表/详情/复习队列/标记掌握/同类题推荐")
@SecurityRequirement(name = "TokenAuth")
public class ErrorBookController {

    private final ErrorBookService errorBookService;

    public ErrorBookController(ErrorBookService errorBookService) {
        this.errorBookService = errorBookService;
    }

    @Operation(summary = "错题列表", description = "支持按科目/错误类型筛选 + 分页")
    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> list(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String errorType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var user = UserHolder.getUser();
        return ApiResponse.success(errorBookService.listErrors(
                user.getUserId(), subject, errorType, page, size));
    }

    @Operation(summary = "错题详情", description = "含三路诊断结果 + 同类题推荐")
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable String id) {
        var user = UserHolder.getUser();
        return ApiResponse.success(errorBookService.getErrorDetail(user.getUserId(), id));
    }

    @Operation(summary = "今日复习队列", description = "基于艾宾浩斯遗忘曲线，返回今天需要复习的错题")
    @GetMapping("/review-queue")
    public ApiResponse<List<Map<String, Object>>> reviewQueue() {
        var user = UserHolder.getUser();
        return ApiResponse.success(errorBookService.getReviewQueue(user.getUserId()));
    }

    @Operation(summary = "标记掌握", description = "将错题标记为已掌握，停止复习推送")
    @PutMapping("/{id}/mark-mastered")
    public ApiResponse<Map<String, Object>> markMastered(@PathVariable String id) {
        var user = UserHolder.getUser();
        errorBookService.markMastered(user.getUserId(), id);
        return ApiResponse.success(Map.of("id", id, "mastered", true));
    }

    @Operation(summary = "取消掌握", description = "将已掌握的错题重新加入复习队列")
    @PutMapping("/{id}/unmark-mastered")
    public ApiResponse<Map<String, Object>> unmarkMastered(@PathVariable String id) {
        var user = UserHolder.getUser();
        errorBookService.unmarkMastered(user.getUserId(), id);
        return ApiResponse.success(Map.of("id", id, "mastered", false));
    }

    @Operation(summary = "同类题推荐", description = "基于错题考点推荐变式题")
    @PostMapping("/{id}/similar")
    public ApiResponse<List<Map<String, Object>>> similar(@PathVariable String id) {
        var user = UserHolder.getUser();
        return ApiResponse.success(errorBookService.getSimilarQuestions(user.getUserId(), id));
    }
}
