package com.suiyuan.iragent.controller;

import cn.hutool.core.bean.BeanUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suiyuan.iragent.dag.core.NodeResult;
import com.suiyuan.iragent.dto.request.DiagnosisRequest;
import com.suiyuan.iragent.service.DiagnosisService;
import com.suiyuan.iragent.service.DiagnosisTimelineService;
import com.suiyuan.iragent.tenant.TenantSemaphoreRegistry;
import com.suiyuan.iragent.utils.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/diagnosis")
@Tag(name = "错题诊断", description = "DAG 驱动错题根因诊断 —— 输入学生错误答案，三路 AI 并行分析根因")
@SecurityRequirement(name = "TokenAuth")
public class DiagnosisController {

    private final DiagnosisService diagnosisService;
    private final DiagnosisTimelineService timelineService;
    private final TenantSemaphoreRegistry semaphoreRegistry;
    private final ObjectMapper objectMapper;

    public DiagnosisController(DiagnosisService diagnosisService,
                                DiagnosisTimelineService timelineService,
                                TenantSemaphoreRegistry semaphoreRegistry,
                                ObjectMapper objectMapper) {
        this.diagnosisService = diagnosisService;
        this.timelineService = timelineService;
        this.semaphoreRegistry = semaphoreRegistry;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "SSE 流式错题诊断",
            description = "多租户限流：默认每个租户最多 5 个并发诊断，超出返回 429")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDiagnosis(@Valid @RequestBody DiagnosisRequest request) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        var currentUser = UserHolder.getUser();
        if (currentUser == null || currentUser.getUserId() == null) {
            sendError(emitter, "用户未登录");
            return emitter;
        }

        String tenantId = "tenant-" + currentUser.getUserId();

        // SSE 异步模式下在 Controller 内管理 Semaphore 生命周期，不在 Interceptor
        if (!semaphoreRegistry.tryAcquire(tenantId, 1000)) {
            sendError(emitter, "当前使用人数过多，请稍后重试", 429);
            return emitter;
        }

        emitter.onCompletion(() -> semaphoreRegistry.release(tenantId));
        emitter.onError(e -> semaphoreRegistry.release(tenantId));
        emitter.onTimeout(() -> semaphoreRegistry.release(tenantId));

        Thread.startVirtualThread(() -> {
            try {
                sendEvent(emitter, "start", Map.of("message", "三路 AI 并行开始分析，逐字流式输出..."));

                Map<String, NodeResult> results = diagnosisService.diagnose(
                        request.getQuestion(),
                        request.getStudentAnswer(),
                        request.getSubjectType(),
                        String.valueOf(currentUser.getUserId()),
                        (nodeId, textChunk) -> {
                            Map<String, Object> chunkData = new HashMap<>();
                            chunkData.put("nodeId", nodeId);
                            chunkData.put("content", textChunk);
                            sendEvent(emitter, "chunk", chunkData);
                        },
                        nodeResult -> {
                            Map<String, Object> eventData = BeanUtil.beanToMap(nodeResult, "output", "error");
                            sendEvent(emitter, "node_complete", eventData);
                        }
                );

                List<Map<String, Object>> timeline = timelineService.convertToTimeline(results);

                Map<String, Object> timelineData = new HashMap<>();
                timelineData.put("actions", timeline);
                timelineData.put("nodeCount", results.size());
                sendEvent(emitter, "timeline", timelineData);

                emitter.complete();
                log.info("DAG 诊断完成: userId={}", currentUser.getUserId());

            } catch (Exception e) {
                log.error("诊断执行失败", e);
                sendError(emitter, "诊断失败: " + e.getMessage());
            }
        });

        return emitter;
    }

    private void sendEvent(SseEmitter emitter, String type, Object data) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("type", type);
            event.put("data", data);
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(event)));
        } catch (IllegalStateException e) {
            // 客户端已断连，emitter 已关闭——并发场景下的正常情况，静默跳过
        } catch (Exception e) {
            log.error("SSE 事件发送失败: type={}", type, e);
        }
    }

    private void sendError(SseEmitter emitter, String message) {
        sendError(emitter, message, 500);
    }

    private void sendError(SseEmitter emitter, String message, int status) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("type", "error");
            event.put("code", status);
            event.put("data", Map.of("message", message));
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(event)));
            emitter.complete();
        } catch (Exception e) {
            log.error("SSE 错误事件发送失败", e);
        }
    }
}
