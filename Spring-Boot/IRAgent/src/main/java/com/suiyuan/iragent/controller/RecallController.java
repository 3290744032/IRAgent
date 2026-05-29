package com.suiyuan.iragent.controller;

import com.suiyuan.iragent.rag.retrieval.AdaptiveRecallService;
import com.suiyuan.iragent.rag.retrieval.RrfRanker.RankedResult;
import com.suiyuan.iragent.utils.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/recall")
@Tag(name = "真题召回", description = "基于学情画像的自适应真题召回")
@SecurityRequirement(name = "TokenAuth")
public class RecallController {

    private final AdaptiveRecallService recallService;

    public RecallController(AdaptiveRecallService recallService) {
        this.recallService = recallService;
    }

    @Operation(summary = "自适应真题召回", description = "根据学生薄弱考点标签 + 当前题目，从向量库中召回最相关的真题")
    @PostMapping("/adaptive")
    public ApiResponse<List<RankedResult>> adaptiveRecall(@RequestBody Map<String, Object> body) {
        String query = (String) body.get("query");
        @SuppressWarnings("unchecked")
        List<String> weakTags = (List<String>) body.get("weakTags");
        int topK = body.get("topK") != null ? ((Number) body.get("topK")).intValue() : 10;

        List<RankedResult> results = recallService.recall(query, weakTags, topK);
        return ApiResponse.success(results);
    }
}
