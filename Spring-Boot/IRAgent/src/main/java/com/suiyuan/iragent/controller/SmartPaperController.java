package com.suiyuan.iragent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suiyuan.iragent.service.SmartPaperService;
import com.suiyuan.iragent.service.SmartPaperStreamService;
import com.suiyuan.iragent.utils.ApiResponse;
import com.suiyuan.iragent.utils.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v3/paper")
@Tag(name = "智能组卷", description = "自适应组卷——根据薄弱考点+难度分布+题型比例生成试卷")
@SecurityRequirement(name = "TokenAuth")
public class SmartPaperController {

    private final SmartPaperService service;
    private final SmartPaperStreamService streamService;
    private final ObjectMapper objectMapper;

    public SmartPaperController(SmartPaperService service,
                                 SmartPaperStreamService streamService,
                                 ObjectMapper objectMapper) {
        this.service = service;
        this.streamService = streamService;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "生成试卷")
    @PostMapping("/smart")
    public ApiResponse<Map<String, Object>> generate(@RequestBody Map<String, Object> body) {
        var user = UserHolder.getUser();
        String subject = (String) body.getOrDefault("subject", "数学");
        String examType = (String) body.get("examType");
        String title = (String) body.getOrDefault("title", "薄弱考点专项练习");
        int questionCount = body.get("questionCount") instanceof Number n ? n.intValue() : 10;
        int difficulty = body.get("difficulty") instanceof Number n ? n.intValue() : 3;
        boolean excludeDone = body.get("excludeDone") instanceof Boolean b ? b : true;

        @SuppressWarnings("unchecked")
        java.util.List<String> kps = (java.util.List<String>) body.get("knowledgePoints");

        return ApiResponse.success(service.generatePaper(user.getUserId(), subject, examType,
                title, questionCount, difficulty, kps, excludeDone));
    }

    @Operation(summary = "SSE 流式智能组卷（对话式）",
            description = "用户输入自然语言，AI 流式生成试卷内容，完成后发送 complete 事件包含 paperId")
    @PostMapping(value = "/smart/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSmartPaper(@RequestBody Map<String, Object> body) {
        String prompt = (String) body.get("prompt");
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        var user = UserHolder.getUser();
        if (user == null || user.getUserId() == null) {
            sendEvent(emitter, "error", Map.of("message", "用户未登录"));
            emitter.complete();
            return emitter;
        }

        Thread.startVirtualThread(() -> {
            try {
                streamService.streamGeneratePaper(
                        user.getUserId(), prompt,
                        chunk -> sendEvent(emitter, "chunk", Map.of("content", chunk)),
                        result -> {
                            sendEvent(emitter, "complete", result);
                            emitter.complete();
                        },
                        error -> {
                            sendEvent(emitter, "error", Map.of("message", error.getMessage()));
                            emitter.complete();
                        }
                );
            } catch (Exception e) {
                log.error("流式组卷失败", e);
                sendEvent(emitter, "error", Map.of("message", e.getMessage()));
                emitter.complete();
            }
        });

        return emitter;
    }

    @Operation(summary = "提交组卷答案")
    @PostMapping("/submit")
    public ApiResponse<Map<String, Object>> submit(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(service.submitAnswers(UserHolder.getUser().getUserId(), body));
    }

    private void sendEvent(SseEmitter emitter, String type, Object data) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("type", type);
            event.put("data", data);
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(event)));
        } catch (Exception e) {
            log.error("SSE 发送失败: type={}", type, e);
        }
    }
}
