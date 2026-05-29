package com.suiyuan.iragent.controller;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suiyuan.iragent.config.VolcEngineStreamingClient;
import com.suiyuan.iragent.dto.request.GradingRequest;
import com.suiyuan.iragent.dto.response.GradingReportResponse;
import com.suiyuan.iragent.service.GradingPipelineService;
import com.suiyuan.iragent.utils.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v3/grading")
@Tag(name = "试卷批改", description = "AI 试卷批改——文本/图片上传，SSE 流式进度")
@SecurityRequirement(name = "TokenAuth")
public class GradingController {

    private final GradingPipelineService gradingService;
    private final VolcEngineStreamingClient streamingClient;
    private final ObjectMapper objectMapper;
    private static final ObjectMapper lenientMapper = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);

    public GradingController(GradingPipelineService gradingService,
                             VolcEngineStreamingClient streamingClient, ObjectMapper objectMapper) {
        this.gradingService = gradingService;
        this.streamingClient = streamingClient;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "提交试卷批改（SSE）",
            description = "提交试卷文本，SSE 流式推送 4 步进度：ocr→extract→grade→diagnose→complete")
    @PostMapping(value = "/submit", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter submit(@Valid @RequestBody GradingRequest request) {
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);

        var user = UserHolder.getUser();
        if (user == null || user.getUserId() == null) {
            sendEvent(emitter, "error", Map.of("message", "用户未登录"));
            emitter.complete();
            return emitter;
        }

        Thread.startVirtualThread(() -> {
            try {
                GradingReportResponse report = gradingService.grade(
                        request.getContent(),
                        request.getSubjectType(),
                        request.getMaxScore(),
                        user.getUserId(),
                        step -> sendEvent(emitter, "step", step)
                );

                sendEvent(emitter, "complete", report);
                emitter.complete();
                log.info("批改完成: userId={}, score={}/{}, accuracy={}%",
                        user.getUserId(), report.getTotalScore(), report.getMaxScore(),
                        String.format("%.1f", report.getAccuracy() * 100));

            } catch (Exception e) {
                log.error("批改失败", e);
                sendEvent(emitter, "error", Map.of("message", e.getMessage()));
                emitter.complete();
            }
        });

        return emitter;
    }

    @Operation(summary = "拍照批改（多模态 SSE）",
            description = "上传试卷图片，豆包多模态模型识别题目+学生答案并批改")
    @PostMapping(value = "/submit-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter submitImage(@RequestParam("image") MultipartFile image,
                                   @RequestParam(defaultValue = "数学") String subject,
                                   @RequestParam(defaultValue = "100") int maxScore) {
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);

        var user = UserHolder.getUser();
        if (user == null || user.getUserId() == null) {
            sendEvent(emitter, "error", Map.of("message", "用户未登录"));
            emitter.complete();
            return emitter;
        }

        Thread.startVirtualThread(() -> {
            try {
                sendEvent(emitter, "step", Map.of("step", "ocr", "text", "豆包视觉识别试卷中...", "current", 0, "total", 0));
                String base64 = Base64.getEncoder().encodeToString(image.getBytes());

                String prompt = "你是阅卷老师。请识别照片中题目和学生手写答案，逐题批改。" +
                        "科目：" + subject + "，满分：" + maxScore + "。" +
                        "输出严格JSON（不要任何其他文字）：" +
                        "{\"totalScore\":得分,\"maxScore\":" + maxScore + ",\"correctCount\":正确数,\"wrongCount\":错误数,\"accuracy\":正确率," +
                        "\"questions\":[{\"index\":题号,\"questionText\":\"题目\",\"studentAnswer\":\"学生答案\",\"correctAnswer\":\"正确答案\",\"isCorrect\":true/false,\"score\":得分,\"maxScore\":满分,\"knowledgePoint\":\"考点\",\"explanation\":\"解析\"}]}";

                sendEvent(emitter, "step", Map.of("step", "extract", "text", "提取题目中...", "current", 0, "total", 0));
                sendEvent(emitter, "step", Map.of("step", "grade", "text", "AI逐题批改中...", "current", 0, "total", 0));

                StringBuilder fullText = new StringBuilder();
                streamingClient.streamChatWithImage(prompt, base64,
                        text -> {
                            fullText.append(text);
                            sendEvent(emitter, "chunk", Map.of("content", text));
                        },
                        () -> {
                            sendEvent(emitter, "step", Map.of("step", "diagnose", "text", "诊断中...", "current", 0, "total", 0));
                            try {
                                String raw = fullText.toString();
                                String json = extractJson(raw);
                                if (json != null) {
                                    try {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> result = lenientMapper.readValue(escapeLatexBackslashes(json), Map.class);
                                        // 重新统计，不信 AI 的摘要和每题得分
                                        @SuppressWarnings("unchecked")
                                        List<Map<String, Object>> questions = (List<Map<String, Object>>) result.get("questions");
                                        int correctCount = 0, wrongCount = 0, totalScore = 0;
                                        int qSize = questions == null ? 0 : questions.size();
                                        int scorePerQuestion = qSize > 0 ? maxScore / qSize : 0;
                                        if (questions != null) {
                                            for (Map<String, Object> q : questions) {
                                                boolean isCorrect = Boolean.TRUE.equals(q.get("isCorrect"));
                                                if (isCorrect) {
                                                    correctCount++;
                                                    totalScore += scorePerQuestion;
                                                } else {
                                                    wrongCount++;
                                                }
                                                q.put("score", isCorrect ? scorePerQuestion : 0);
                                                q.put("maxScore", scorePerQuestion);
                                            }
                                        }
                                        result.put("correctCount", correctCount);
                                        result.put("wrongCount", wrongCount);
                                        result.put("totalScore", totalScore);
                                        double accuracy = qSize == 0 ? 0 : (double) correctCount / qSize;
                                        result.put("accuracy", accuracy);
                                        // 对错题执行诊断
                                        String reportId = wrongCount > 0
                                                ? gradingService.diagnoseWrongQuestions(questions,
                                                        user.getUserId(), subject,
                                                        step -> sendEvent(emitter, "step", step))
                                                : java.util.UUID.randomUUID().toString().substring(0, 16);
                                        gradingService.saveReport(reportId, user.getUserId(), subject, maxScore,
                                                totalScore, correctCount, wrongCount, accuracy, questions);
                                        sendEvent(emitter, "complete", result);
                                        emitter.complete();
                                        return;
                                    } catch (Exception e) {
                                        log.warn("AI 返回的 JSON 解析失败，降级为文本批改: {}", e.getMessage());
                                        log.debug("原始 AI 响应: {}", raw);
                                    }
                                }
                                // JSON 不完整或解析失败 → 走文本批改降级
                                sendEvent(emitter, "step", Map.of("step", "grade", "text", "降级为文本批改...", "current", 0, "total", 0));
                                GradingReportResponse report = gradingService.grade(
                                        raw, subject, maxScore, user.getUserId(), s -> {});
                                sendEvent(emitter, "complete", report);
                            } catch (Exception e) {
                                log.error("批改结果解析失败", e);
                                sendEvent(emitter, "error", Map.of("message", "批改结果解析失败"));
                            }
                            emitter.complete();
                        },
                        error -> {
                            sendEvent(emitter, "error", Map.of("message", error.getMessage()));
                            emitter.complete();
                        }
                );
            } catch (Exception e) {
                log.error("拍照批改失败", e);
                sendEvent(emitter, "error", Map.of("message", e.getMessage()));
                emitter.complete();
            }
        });

        return emitter;
    }

    private String extractJson(String text) {
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start >= 0 && end > start) return text.substring(start, end + 1);
        return null;
    }

    /**
     * AI 经常忘记转义 LaTeX 中的反斜杠（如 \frac、\lim），
     * 导致 JSON 解析失败或 \f → 换页符、\t → 制表符等乱码。
     * 此方法将非法的单反斜杠补成双反斜杠，保留合法转义序列。
     */
    private String escapeLatexBackslashes(String json) {
        StringBuilder sb = new StringBuilder(json.length());
        int len = json.length();
        for (int i = 0; i < len; i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < len) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"': case '\\': case '/':
                    case 'b': case 'f': case 'n': case 'r': case 't':
                        // 合法 JSON 转义，保持原样
                        sb.append(c).append(next);
                        i++;
                        break;
                    case 'u':
                        // \\uXXXX is valid Unicode escape; check 4 hex digits follow
                        if (i + 5 < len && isHexDigit(json.charAt(i + 2))
                                && isHexDigit(json.charAt(i + 3))
                                && isHexDigit(json.charAt(i + 4))
                                && isHexDigit(json.charAt(i + 5))) {
                            sb.append(c).append(next);
                            i++;
                            for (int j = 0; j < 4; j++) {
                                sb.append(json.charAt(++i));
                            }
                        } else {
                            // invalid \\u, treat as LaTeX
                            sb.append('\\').append('\\').append(next);
                            i++;
                        }
                        break;
                    default:
                        // 非法转义序列（如 LaTeX 的 \frac、\lim），补双反斜杠
                        sb.append('\\').append('\\').append(next);
                        i++;
                        break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
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
