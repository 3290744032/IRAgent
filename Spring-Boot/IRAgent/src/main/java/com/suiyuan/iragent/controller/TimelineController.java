package com.suiyuan.iragent.controller;

import com.suiyuan.iragent.config.VolcEngineChatClient;
import com.suiyuan.iragent.entity.Conversation;
import com.suiyuan.iragent.entity.TeachingTimeline;
import com.suiyuan.iragent.service.ConversationService;
import com.suiyuan.iragent.service.TeachingPlanService;
import com.suiyuan.iragent.utils.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/timeline")
@RequiredArgsConstructor
public class TimelineController {

    private final TeachingPlanService teachingPlanService;
    private final VolcEngineChatClient volcEngineChatClient;
    private final ConversationService conversationService;

    /**
     * 简单的标题生成接口（只生成标题，不生成时间轴）
     * @param request 包含 topic（题目）和可选的 conversationId（对话ID）
     */
    @PostMapping("/title")
    public ApiResponse<Map<String, Object>> generateTitle(@RequestBody Map<String, Object> request) {
        try {
            String topic = (String) request.get("topic");
            if (topic == null || topic.trim().isEmpty()) {
                return ApiResponse.badRequest("题目不能为空");
            }

            String conversationId = (String) request.get("conversationId");

            log.info("[TimelineController] 生成标题: topic={}, conversationId={}", topic, conversationId);

            // 生成简短标题（豆包风格）
            String prompt = String.format("""
                根据数学题目生成简洁标题，6-12字。

                题目：%s

                要求：
                1. 6-12字，非常简洁
                2. 用动词开头
                3. 直接点出核心知识点
                4. 不要废话
                5. 只输出标题，不要其他内容

                输出：
                """, topic);

            String title = volcEngineChatClient.chat(prompt);

            // 清理标题
            title = title.trim()
                    .replaceAll("^[\"'`]", "")
                    .replaceAll("[\"'`]$", "")
                    .replaceAll("[。，、]", "")
                    .trim();

            // 确保标题长度在合理范围
            if (title.length() < 6) {
                title = topic.length() > 12 ? topic.substring(0, 12) : topic;
            } else if (title.length() > 20) {
                title = title.substring(0, 20);
            }

            log.info("[TimelineController] 标题生成成功: topic={}, title={}", topic, title);

            // 如果传入了conversationId，保存到数据库
            boolean saved = false;
            if (conversationId != null && !conversationId.trim().isEmpty()) {
                try {
                    Conversation conversation = conversationService.getConversationById(conversationId);
                    if (conversation != null) {
                        conversation.setName(title);
                        conversation.setUpdatedAt(LocalDateTime.now());
                        conversationService.updateConversation(conversation);
                        saved = true;
                        log.info("[TimelineController] 标题已保存到会话: conversationId={}, title={}", conversationId, title);
                    }
                } catch (Exception e) {
                    log.warn("[TimelineController] 保存标题到会话失败: {}", e.getMessage());
                }
            }

            return ApiResponse.success("标题生成成功", Map.of(
                    "title", title,
                    "saved", saved,
                    "conversationId", conversationId
            ));
        } catch (Exception e) {
            log.error("[TimelineController] 标题生成失败: {}", e.getMessage(), e);
            return ApiResponse.error("标题生成失败: " + e.getMessage());
        }
    }

    @PostMapping("/generate")
    public ApiResponse<TeachingTimeline> generateTimeline(@RequestBody Map<String, Object> request) {
        try {
            String topic = (String) request.get("topic");
            if (topic == null || topic.trim().isEmpty()) {
                return ApiResponse.badRequest("题目不能为空");
            }

            log.info("[TimelineController] 开始生成教学时间轴: topic={}", topic);

            long startTime = System.currentTimeMillis();
            TeachingTimeline timeline = teachingPlanService.generateTimeline(topic);
            long elapsed = System.currentTimeMillis() - startTime;

            log.info("[TimelineController] 教学时间轴生成完成: topic={}, duration={}s, actions={}, 耗时={}ms",
                    topic, timeline.getDurationSeconds(), timeline.getTimeline().size(), elapsed);

            return ApiResponse.success("教学时间轴生成完成", timeline);
        } catch (IllegalArgumentException e) {
            log.warn("[TimelineController] 参数错误: {}", e.getMessage());
            return ApiResponse.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("[TimelineController] 生成失败: {}", e.getMessage(), e);
            return ApiResponse.error("生成教学时间轴失败: " + e.getMessage());
        }
    }
}
