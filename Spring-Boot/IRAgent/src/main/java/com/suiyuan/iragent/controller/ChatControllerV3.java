package com.suiyuan.iragent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suiyuan.iragent.service.ConversationService;
import com.suiyuan.iragent.service.NoteAnchoredChatService;
import com.suiyuan.iragent.utils.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/v3/chat")
@Tag(name = "答疑 V3", description = "笔记锚定答疑 + 多模态拍照解题")
@SecurityRequirement(name = "TokenAuth")
public class ChatControllerV3 {

    private final NoteAnchoredChatService chatService;
    private final com.suiyuan.iragent.config.VolcEngineStreamingClient streamingClient;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;

    public ChatControllerV3(NoteAnchoredChatService chatService,
                             com.suiyuan.iragent.config.VolcEngineStreamingClient streamingClient,
                             ConversationService conversationService,
                             ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.streamingClient = streamingClient;
        this.conversationService = conversationService;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "SSE 流式答疑（笔记锚定）",
            description = "AI 回答时自动检索用户笔记并注入上下文，回答末尾附带 noteRefs 引用数组。" +
                    "前端根据 noteRefs 渲染笔记引用卡片。")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody Map<String, Object> body) {
        String question = (String) body.get("question");
        String conversationId = (String) body.getOrDefault("conversationId", "");
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        var user = UserHolder.getUser();
        if (user == null || user.getUserId() == null) {
            sendEvent(emitter, "error", Map.of("message", "用户未登录"));
            emitter.complete();
            return emitter;
        }

        // 生成或沿用 conversationId，SSE 第一个事件就告诉前端
        final String cid = (conversationId != null && !conversationId.isEmpty())
                ? conversationId : "v3-" + java.util.UUID.randomUUID().toString().substring(0, 12);

        Thread.startVirtualThread(() -> {
            // 先发送 meta 事件，包含 conversationId，前端可以据此追踪对话
            sendEvent(emitter, "meta", Map.of("conversationId", cid));

            chatService.chat(
                    user.getUserId(),
                    cid,
                    question,
                    chunk -> sendEvent(emitter, "chunk", Map.of("content", chunk)),
                    noteRefs -> {
                        Map<String, Object> refData = new HashMap<>();
                        refData.put("noteRefs", noteRefs);
                        sendEvent(emitter, "note_refs", refData);
                    },
                    plotConfig -> sendEvent(emitter, "plot", plotConfig),
                    plot3dConfig -> sendEvent(emitter, "plot3d", plot3dConfig),
                    () -> {
                        sendEvent(emitter, "done", Map.of());
                        emitter.complete();
                    },
                    error -> {
                        sendEvent(emitter, "error", Map.of("message", error.getMessage()));
                        emitter.complete();
                    }
            );
        });

        return emitter;
    }

    @Operation(summary = "拍照解题（多模态 SSE）",
            description = "上传图片 + 文字描述，多模态模型识别题目并流式输出解题过程")
    @PostMapping(value = "/stream-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamImage(@RequestParam("image") MultipartFile image,
                                   @RequestParam(defaultValue = "请帮我解答这道题") String question,
                                   @RequestParam(defaultValue = "") String conversationId) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        var user = UserHolder.getUser();
        if (user == null || user.getUserId() == null) {
            sendEvent(emitter, "error", Map.of("message", "用户未登录"));
            emitter.complete();
            return emitter;
        }

        Thread.startVirtualThread(() -> {
            try {
                String base64 = Base64.getEncoder().encodeToString(image.getBytes());
                final String cid = (conversationId != null && !conversationId.isEmpty())
                        ? conversationId : "img-" + java.util.UUID.randomUUID().toString().substring(0, 12);
                sendEvent(emitter, "meta", Map.of("conversationId", cid));

                // 保存用户消息
                try {
                    conversationService.sendMessage(cid, "user", question, "text");
                } catch (Exception e) {
                    conversationService.createConversation(user.getUserId(),
                            question.length() > 50 ? question.substring(0, 50) : question, "拍照解题");
                    conversationService.sendMessage(cid, "user", question, "text");
                }

                StringBuilder fullText = new StringBuilder();
                streamingClient.streamChatWithImage(
                        question,
                        base64,
                        text -> {
                            fullText.append(text);
                            sendEvent(emitter, "chunk", Map.of("content", text));
                        },
                        () -> {
                            String response = fullText.toString();
                            // 保存 AI 回复
                            try {
                                conversationService.sendMessage(cid, "ai",
                                        response.length() > 5000 ? response.substring(0, 5000) : response, "text");
                            } catch (Exception e) {
                                log.error("保存拍照解题AI回复失败: {}", e.getMessage());
                            }

                            // 解析完整回答中的 PLOT/PLOT3D 块
                            var segments = com.suiyuan.iragent.utils.ContentParser.parse(response);
                            for (var seg : segments) {
                                if (seg.isPlot() && seg.getPlotConfiguration() != null)
                                    sendEvent(emitter, "plot", seg.getPlotConfiguration());
                                else if (seg.isPlot3d() && seg.getPlot3DConfiguration() != null)
                                    sendEvent(emitter, "plot3d", seg.getPlot3DConfiguration());
                            }
                            sendEvent(emitter, "done", Map.of());
                            emitter.complete();
                        },
                        error -> {
                            sendEvent(emitter, "error", Map.of("message", error.getMessage()));
                            emitter.complete();
                        }
                );
            } catch (Exception e) {
                log.error("拍照解题失败", e);
                sendEvent(emitter, "error", Map.of("message", e.getMessage()));
                emitter.complete();
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
        } catch (Exception e) {
            log.error("SSE 发送失败: type={}", type, e);
        }
    }
}
