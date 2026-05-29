package com.suiyuan.iragent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suiyuan.iragent.config.VolcEngineStreamingClient;
import com.suiyuan.iragent.dto.request.AnswerRequest;
import com.suiyuan.iragent.dto.request.CreateSessionRequest;
import com.suiyuan.iragent.dto.response.CreateSessionResponse;
import com.suiyuan.iragent.service.LearningSessionService;
import com.suiyuan.iragent.utils.ApiResponse;
import com.suiyuan.iragent.utils.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Tag(name = "深度学习模块", description = "基于苏格拉底式教学的多轮对话深度学习模块")
@SecurityRequirement(name = "TokenAuth")
@RestController
@RequestMapping("/v2/sessions")
@RequiredArgsConstructor
public class LearningController {

    private final LearningSessionService learningSessionService;
    private final VolcEngineStreamingClient volcEngineStreamingClient;
    private final ObjectMapper objectMapper;

    @Operation(summary = "获取学习历史", description = "获取当前用户的所有学习会话历史（分页）")
    @GetMapping("/history")
    public ApiResponse<Map<String, Object>> getSessionHistory(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        try {
            var currentUser = UserHolder.getUser();
            if (currentUser == null || currentUser.getUserId() == null) {
                return ApiResponse.unauthorized("用户未登录或token已过期，请重新登录");
            }
            Map<String, Object> result = learningSessionService.getSessionHistory(currentUser.getUserId(), page, size);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("获取学习历史失败", e);
            return ApiResponse.error("获取学习历史失败: " + e.getMessage());
        }
    }

    @Operation(summary = "创建学习会话", description = "用户提出问题，系统创建学习会话")
    @PostMapping
    public ApiResponse<CreateSessionResponse> createSession(@RequestBody CreateSessionRequest request) {
        try {
            var currentUser = UserHolder.getUser();
            if (currentUser == null || currentUser.getUserId() == null) {
                return ApiResponse.unauthorized("用户未登录或token已过期，请重新登录");
            }

            log.info("创建学习会话: userId={}, question={}", currentUser.getUserId(), request.getQuestion());
            CreateSessionResponse result = learningSessionService.createSession(
                    currentUser.getUserId(),
                    request.getQuestion(),
                    request.getSubjectType()
            );

            learningSessionService.clearHistory(result.getSessionId());

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("创建学习会话失败", e);
            return ApiResponse.error("创建学习会话失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取会话详情", description = "获取指定学习会话的详细信息")
    @GetMapping("/{sessionId}")
    public ApiResponse<Map<String, Object>> getSessionDetail(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {
        try {
            var currentUser = UserHolder.getUser();
            if (currentUser == null || currentUser.getUserId() == null) {
                return ApiResponse.unauthorized("用户未登录或token已过期，请重新登录");
            }
            Map<String, Object> detail = learningSessionService.getSessionDetail(sessionId);
            return ApiResponse.success(detail);
        } catch (Exception e) {
            log.error("获取会话详情失败: sessionId={}", sessionId, e);
            return ApiResponse.error("获取会话详情失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取学习总结", description = "获取指定学习会话的AI总结，包含知识图谱、掌握度分析、学习建议等")
    @GetMapping("/{sessionId}/summary")
    public ApiResponse<Map<String, Object>> getSessionSummary(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {
        try {
            var currentUser = UserHolder.getUser();
            if (currentUser == null || currentUser.getUserId() == null) {
                return ApiResponse.unauthorized("用户未登录或token已过期，请重新登录");
            }
            Map<String, Object> summary = learningSessionService.getSessionSummary(sessionId);
            return ApiResponse.success(summary);
        } catch (RuntimeException e) {
            log.warn("获取学习总结失败: sessionId={}, {}", sessionId, e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("获取学习总结失败: sessionId={}", sessionId, e);
            return ApiResponse.error("获取学习总结失败: " + e.getMessage());
        }
    }

    @Operation(summary = "SSE流式讲解（多轮对话）", description = "每次调用只讲一个模块，讲完后等待用户回答再调用下一轮")
    @GetMapping(value = "/{sessionId}/teach", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startTeaching(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {

        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        try {
            var currentUser = UserHolder.getUser();
            if (currentUser == null || currentUser.getUserId() == null) {
                emitter.send(SseEmitter.event().data("{\"type\":\"error\",\"code\":401,\"message\":\"用户未登录或token已过期\"}"));
                emitter.complete();
                return emitter;
            }

            int round = learningSessionService.getCurrentRound(sessionId);
            String question = learningSessionService.getQuestion(sessionId);
            String prompt = learningSessionService.buildTeachPrompt(sessionId, question);

            log.info("SSE流式讲解: sessionId={}, round={}", sessionId, round);

            int displayRound = round + 1;
            boolean isLastModule = round >= 3; // MODULE_NAMES.length - 1 = 3
            emitter.send(SseEmitter.event().data(
                    "{\"type\":\"start\",\"round\":" + displayRound + ",\"message\":\"第" + displayRound + "模块讲解开始\"}"));

            StringBuilder fullResponse = new StringBuilder();

            Thread.startVirtualThread(() -> {
                try {
                    volcEngineStreamingClient.streamChat(
                            prompt,
                            text -> {
                                try {
                                    fullResponse.append(text);
                                    String safeContent = escapeJson(text);
                                    emitter.send(SseEmitter.event().data(
                                            "{\"type\":\"text\",\"content\":\"" + safeContent + "\"}"));
                                } catch (IOException e) {
                                    log.error("发送流式响应失败", e);
                                }
                            },
                            () -> {
                                try {
                                    String teacherContent = fullResponse.toString();
                                    learningSessionService.appendTeacherMessage(sessionId, teacherContent);

                                    String eventType = isLastModule ? "completed" : "done";
                                    emitter.send(SseEmitter.event().data(
                                            "{\"type\":\"" + eventType + "\",\"round\":" + displayRound + "}"));
                                    emitter.complete();
                                    log.info("第{}轮讲解完成: sessionId={}, 内容长度={}, isLastModule={}", displayRound, sessionId, teacherContent.length(), isLastModule);
                                } catch (IOException e) {
                                    log.error("发送完成信号失败", e);
                                }
                            },
                            error -> {
                                try {
                                    String errorMessage = escapeJson(error.getMessage());
                                    emitter.send(SseEmitter.event().data(
                                            "{\"type\":\"error\",\"code\":500,\"message\":\"AI服务调用失败: " + errorMessage + "\"}"));
                                    emitter.complete();
                                } catch (IOException e) {
                                    log.error("发送错误信号失败", e);
                                }
                            }
                    );
                } catch (Exception e) {
                    log.error("流式讲解处理失败", e);
                    try {
                        String errorMessage = escapeJson(e.getMessage());
                        emitter.send(SseEmitter.event().data(
                                "{\"type\":\"error\",\"code\":500,\"message\":\"讲解生成失败: " + errorMessage + "\"}"));
                        emitter.complete();
                    } catch (IOException ex) {
                        log.error("发送异常信号失败", ex);
                    }
                }
            });

        } catch (Exception e) {
            log.error("SSE流式讲解初始化失败", e);
            try {
                String errorMessage = escapeJson(e.getMessage());
                emitter.send(SseEmitter.event().data(
                        "{\"type\":\"error\",\"code\":500,\"message\":\"讲解生成失败: " + errorMessage + "\"}"));
                emitter.complete();
            } catch (IOException ex) {
                log.error("发送异常信号失败", ex);
            }
        }

        return emitter;
    }

    @Operation(summary = "SSE流式总结", description = "流式生成学习总结，包含知识图谱、掌握度分析、学习建议等")
    @GetMapping(value = "/{sessionId}/summary/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSummary(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {

        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        try {
            var currentUser = UserHolder.getUser();
            if (currentUser == null || currentUser.getUserId() == null) {
                emitter.send(SseEmitter.event().data("{\"type\":\"error\",\"code\":401,\"message\":\"用户未登录或token已过期\"}"));
                emitter.complete();
                return emitter;
            }

            if (learningSessionService.hasSummaryCache(sessionId)) {
                Map<String, Object> cachedSummary = learningSessionService.getSessionSummary(sessionId);
                String jsonData = objectMapper.writeValueAsString(cachedSummary);
                String event = "{\"type\":\"summary\",\"data\":" + jsonData + "}";
                emitter.send(SseEmitter.event().data(event));
                emitter.complete();
                log.info("SSE总结（已缓存）: sessionId={}", sessionId);
                return emitter;
            }

            String prompt = learningSessionService.buildSummaryPrompt(sessionId);
            log.info("SSE流式总结初始化: sessionId={}", sessionId);

            emitter.send(SseEmitter.event().data("{\"type\":\"start\",\"message\":\"正在生成学习总结...\"}"));

            StringBuilder fullResponse = new StringBuilder();

            Thread.startVirtualThread(() -> {
                try {
                    volcEngineStreamingClient.streamChat(
                            prompt,
                            text -> {
                                try {
                                    fullResponse.append(text);
                                    String safeContent = escapeJson(text);
                                    emitter.send(SseEmitter.event().data(
                                            "{\"type\":\"text\",\"content\":\"" + safeContent + "\"}"));
                                } catch (IOException e) {
                                    log.error("发送总结流式响应失败", e);
                                }
                            },
                            () -> {
                                try {
                                    String rawResponse = fullResponse.toString();
                                    Map<String, Object> summaryData = learningSessionService.saveGeneratedSummary(sessionId, rawResponse);
                                    String jsonData = objectMapper.writeValueAsString(summaryData);
                                    emitter.send(SseEmitter.event().data(
                                            "{\"type\":\"summary\",\"data\":" + jsonData + "}"));
                                    emitter.complete();
                                    log.info("SSE流式总结完成: sessionId={}", sessionId);
                                } catch (Exception e) {
                                    log.error("保存总结数据失败: sessionId={}", sessionId, e);
                                    try {
                                        String errorMessage = escapeJson(e.getMessage());
                                        emitter.send(SseEmitter.event().data(
                                                "{\"type\":\"error\",\"code\":500,\"message\":\"保存总结数据失败: " + errorMessage + "\"}"));
                                        emitter.complete();
                                    } catch (IOException ex) {
                                        log.error("发送保存失败信号异常", ex);
                                    }
                                }
                            },
                            error -> {
                                try {
                                    String errorMessage = escapeJson(error.getMessage());
                                    emitter.send(SseEmitter.event().data(
                                            "{\"type\":\"error\",\"code\":500,\"message\":\"AI服务调用失败: " + errorMessage + "\"}"));
                                    emitter.complete();
                                } catch (IOException e) {
                                    log.error("发送错误信号失败", e);
                                }
                            }
                    );
                } catch (Exception e) {
                    log.error("SSE流式总结处理失败", e);
                    try {
                        String errorMessage = escapeJson(e.getMessage());
                        emitter.send(SseEmitter.event().data(
                                "{\"type\":\"error\",\"code\":500,\"message\":\"总结生成失败: " + errorMessage + "\"}"));
                        emitter.complete();
                    } catch (IOException ex) {
                        log.error("发送异常信号失败", ex);
                    }
                }
            });

        } catch (Exception e) {
            log.error("SSE流式总结初始化失败", e);
            try {
                String errorMessage = escapeJson(e.getMessage());
                emitter.send(SseEmitter.event().data(
                        "{\"type\":\"error\",\"code\":500,\"message\":\"总结生成失败: " + errorMessage + "\"}"));
                emitter.complete();
            } catch (IOException ex) {
                log.error("发送异常信号失败", ex);
            }
        }

        return emitter;
    }

    @Operation(summary = "用户回答问题", description = "用户点击选项按钮后，提交答案，系统记录并进入下一轮")
    @PostMapping(value = "/{sessionId}/answer")
    public ApiResponse<Map<String, Object>> answerQuestion(
            @Parameter(description = "会话ID") @PathVariable String sessionId,
            @RequestBody AnswerRequest request) {

        try {
            var currentUser = UserHolder.getUser();
            if (currentUser == null || currentUser.getUserId() == null) {
                return ApiResponse.unauthorized("用户未登录或token已过期，请重新登录");
            }

            String question = learningSessionService.getQuestion(sessionId);
            learningSessionService.recordAnswer(sessionId, request.getAnswer(), question);

            int nextRound = learningSessionService.getCurrentRound(sessionId);

            log.info("用户回答: sessionId={}, answer={}, nextRound={}", sessionId, request.getAnswer(), nextRound);

            Map<String, Object> result = Map.of(
                    "sessionId", sessionId,
                    "answer", request.getAnswer(),
                    "nextRound", nextRound
            );

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("处理用户回答失败", e);
            return ApiResponse.error("处理回答失败: " + e.getMessage());
        }
    }

    private String escapeJson(String content) {
        if (content == null) return "";
        return content.replace("\\", "\\\\")
                     .replace("\"", "\\\"")
                     .replace("\n", "\\n")
                     .replace("\r", "\\r")
                     .replace("\t", "\\t");
    }
}
