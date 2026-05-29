package com.suiyuan.iragent.controller;

import com.suiyuan.iragent.entity.Conversation;
import com.suiyuan.iragent.entity.Message;
import com.suiyuan.iragent.service.ConversationService;
import com.suiyuan.iragent.utils.ApiResponse;
import com.suiyuan.iragent.utils.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话控制器
 * 用于处理会话相关的API请求
 */
@Slf4j
@Tag(name = "会话管理", description = "会话创建、管理、消息管理等功能")
@SecurityRequirement(name = "TokenAuth")
@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    // ==================== 会话管理 ====================

    /**
     * 创建新会话
     * @param request 创建会话请求
     * @return 创建的会话
     */
    @Operation(summary = "创建新会话", description = "为当前用户创建一个新的聊天会话")
    @PostMapping
    public ApiResponse<Map<String, Object>> createConversation(@RequestBody CreateConversationRequest request) {
        try {
            var currentUser = UserHolder.getUser();
            if (currentUser == null || currentUser.getUserId() == null) {
                return ApiResponse.unauthorized("用户未登录或token已过期，请重新登录");
            }

            var conversation = conversationService.createConversation(
                    currentUser.getUserId(),
                    request.name(),
                    request.description()
            );

            log.info("创建会话成功: userId={}, conversationId={}, name={}",
                    currentUser.getUserId(), conversation.getConversationId(), request.name());

            Map<String, Object> result = new HashMap<>();
            result.put("conversation", conversation);
            result.put("userId", currentUser.getUserId());
            
            return ApiResponse.success(result);

        } catch (Exception e) {
            log.error("创建会话失败", e);
            return ApiResponse.error("创建会话失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的会话列表（分页）
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 会话列表
     */
    @Operation(summary = "获取会话列表", description = "获取当前用户的所有会话，支持分页")
    @GetMapping
    public ApiResponse<List<Conversation>> getConversations(
            @Parameter(description = "页码（从1开始）") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        
        try {
            var currentUser = UserHolder.getUser();
            if (currentUser == null || currentUser.getUserId() == null) {
                return ApiResponse.unauthorized("用户未登录或token已过期，请重新登录");
            }

            // 分页参数校验
            if (page < 1) page = 1;
            if (size < 1) size = 20;
            if (size > 100) size = 100;

            // 获取分页数据
            List<Conversation> conversations = conversationService.getConversationsByUserIdWithPage(
                    currentUser.getUserId(), page, size);
            long total = conversationService.countConversationsByUserId(currentUser.getUserId());

            log.info("获取会话列表成功: userId={}, page={}, size={}, count={}",
                    currentUser.getUserId(), page, size, conversations.size());

            return ApiResponse.success(conversations, total, page, size);

        } catch (Exception e) {
            log.error("获取会话列表失败", e);
            return ApiResponse.error("获取会话列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的所有会话（不分页）
     * @return 会话列表
     */
    @Operation(summary = "获取所有会话", description = "获取当前用户的所有会话，不分页")
    @GetMapping("/all")
    public ApiResponse<List<Conversation>> getAllConversations() {
        
        try {
            var currentUser = UserHolder.getUser();
            if (currentUser == null || currentUser.getUserId() == null) {
                return ApiResponse.unauthorized("用户未登录或token已过期，请重新登录");
            }

            // 获取所有会话
            List<Conversation> conversations = conversationService.getConversationsByUserId(currentUser.getUserId());

            log.info("获取所有会话成功: userId={}, count={}",
                    currentUser.getUserId(), conversations.size());

            return ApiResponse.success(conversations);

        } catch (Exception e) {
            log.error("获取所有会话失败", e);
            return ApiResponse.error("获取所有会话失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话详情
     * @param conversationId 会话ID
     * @return 会话详情
     */
    @Operation(summary = "获取会话详情", description = "根据会话ID获取会话详情")
    @GetMapping("/{conversationId}")
    public ApiResponse<Map<String, Object>> getConversation(@PathVariable String conversationId) {
        try {
            var currentUser = UserHolder.getUser();
            if (currentUser == null || currentUser.getUserId() == null) {
                return ApiResponse.unauthorized("用户未登录或token已过期，请重新登录");
            }

            var conversation = conversationService.getConversationById(conversationId);
            if (conversation == null) {
                return ApiResponse.notFound("会话不存在");
            }

            if (!conversation.getUserId().equals(currentUser.getUserId())) {
                return ApiResponse.forbidden("无权访问该会话");
            }

            log.info("获取会话详情成功: userId={}, conversationId={}",
                    currentUser.getUserId(), conversationId);

            Map<String, Object> result = new HashMap<>();
            result.put("conversation", conversation);
            result.put("userId", currentUser.getUserId());
            
            return ApiResponse.success(result);

        } catch (Exception e) {
            log.error("获取会话详情失败", e);
            return ApiResponse.error("获取会话详情失败: " + e.getMessage());
        }
    }

    /**
     * 更新会话信息
     * @param conversationId 会话ID
     * @param request 更新会话请求
     * @return 更新后的会话
     */
    @Operation(summary = "更新会话信息", description = "更新会话的名称和描述")
    @PutMapping("/{conversationId}")
    public ApiResponse<Map<String, Object>> updateConversation(
            @PathVariable String conversationId, 
            @RequestBody UpdateConversationRequest request) {
        
        try {
            var currentUser = UserHolder.getUser();
            if (currentUser == null || currentUser.getUserId() == null) {
                return ApiResponse.unauthorized("用户未登录或token已过期，请重新登录");
            }

            var conversation = conversationService.getConversationById(conversationId);
            if (conversation == null) {
                return ApiResponse.notFound("会话不存在");
            }

            if (!conversation.getUserId().equals(currentUser.getUserId())) {
                return ApiResponse.forbidden("无权修改该会话");
            }

            conversation.setName(request.name());
            conversation.setDescription(request.description());
            if (request.status() != null) {
                conversation.setStatus(request.status());
            }
            var updatedConversation = conversationService.updateConversation(conversation);

            log.info("更新会话成功: userId={}, conversationId={}",
                    currentUser.getUserId(), conversationId);

            Map<String, Object> result = new HashMap<>();
            result.put("conversation", updatedConversation);
            result.put("userId", currentUser.getUserId());
            
            return ApiResponse.success(result);

        } catch (Exception e) {
            log.error("更新会话失败", e);
            return ApiResponse.error("更新会话失败: " + e.getMessage());
        }
    }

    /**
     * 删除会话
     * @param conversationId 会话ID
     * @return 删除结果
     */
    @Operation(summary = "删除会话", description = "删除指定的会话及其所有消息")
    @DeleteMapping("/{conversationId}")
    public ApiResponse<Map<String, Object>> deleteConversation(@PathVariable String conversationId) {
        try {
            var currentUser = UserHolder.getUser();
            if (currentUser == null || currentUser.getUserId() == null) {
                return ApiResponse.unauthorized("用户未登录或token已过期，请重新登录");
            }

            var conversation = conversationService.getConversationById(conversationId);
            if (conversation == null) {
                return ApiResponse.notFound("会话不存在");
            }

            if (!conversation.getUserId().equals(currentUser.getUserId())) {
                return ApiResponse.forbidden("无权删除该会话");
            }

            boolean result = conversationService.deleteConversation(conversationId);

            log.info("删除会话成功: userId={}, conversationId={}",
                    currentUser.getUserId(), conversationId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result);
            response.put("userId", currentUser.getUserId());
            response.put("conversationId", conversationId);
            
            return ApiResponse.success(response);

        } catch (Exception e) {
            log.error("删除会话失败", e);
            return ApiResponse.error("删除会话失败: " + e.getMessage());
        }
    }

    // ==================== 消息管理 ====================

    /**
     * 获取会话的消息列表（分页）
     * @param conversationId 会话ID
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 消息列表
     */
    @Operation(summary = "获取会话消息", description = "获取指定会话的所有消息，支持分页")
    @GetMapping("/{conversationId}/messages")
    public ApiResponse<List<Message>> getMessages(
            @PathVariable String conversationId,
            @Parameter(description = "页码（从1开始）") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "50") int size) {
        
        try {
            var currentUser = UserHolder.getUser();
            if (currentUser == null || currentUser.getUserId() == null) {
                return ApiResponse.unauthorized("用户未登录或token已过期，请重新登录");
            }

            var conversation = conversationService.getConversationById(conversationId);
            if (conversation == null) {
                return ApiResponse.notFound("会话不存在");
            }

            if (!conversation.getUserId().equals(currentUser.getUserId())) {
                return ApiResponse.forbidden("无权访问该会话的消息");
            }

            // 分页参数校验
            if (page < 1) page = 1;
            if (size < 1) size = 50;
            if (size > 200) size = 200;

            // 获取分页数据
            List<Message> messages = conversationService.getMessagesByConversationIdWithPage(
                    conversationId, page, size);
            long total = conversationService.countMessagesByConversationId(conversationId);

            log.info("获取会话消息成功: userId={}, conversationId={}, page={}, size={}, count={}",
                    currentUser.getUserId(), conversationId, page, size, messages.size());

            return ApiResponse.success(messages, total, page, size);

        } catch (Exception e) {
            log.error("获取会话消息失败", e);
            return ApiResponse.error("获取会话消息失败: " + e.getMessage());
        }
    }

    /**
     * 清除会话的消息
     * @param conversationId 会话ID
     * @return 清除结果
     */
    @Operation(summary = "清除会话消息", description = "清除指定会话的所有消息")
    @DeleteMapping("/{conversationId}/messages")
    public ApiResponse<Map<String, Object>> clearMessages(@PathVariable String conversationId) {
        try {
            var currentUser = UserHolder.getUser();
            if (currentUser == null || currentUser.getUserId() == null) {
                return ApiResponse.unauthorized("用户未登录或token已过期，请重新登录");
            }

            var conversation = conversationService.getConversationById(conversationId);
            if (conversation == null) {
                return ApiResponse.notFound("会话不存在");
            }

            if (!conversation.getUserId().equals(currentUser.getUserId())) {
                return ApiResponse.forbidden("无权操作该会话的消息");
            }

            int deletedCount = conversationService.clearMessagesByConversationId(conversationId);

            log.info("清除会话消息成功: userId={}, conversationId={}, deletedCount={}",
                    currentUser.getUserId(), conversationId, deletedCount);

            Map<String, Object> response = new HashMap<>();
            response.put("deletedCount", deletedCount);
            response.put("conversationId", conversationId);
            response.put("userId", currentUser.getUserId());
            
            return ApiResponse.success(response);

        } catch (Exception e) {
            log.error("清除会话消息失败", e);
            return ApiResponse.error("清除会话消息失败: " + e.getMessage());
        }
    }

    // ==================== 数据传输对象 ====================

    /**
     * 创建会话请求
     */
    public record CreateConversationRequest(String name, String description) {}

    /**
     * 更新会话请求
     */
    public record UpdateConversationRequest(String name, String description, String status) {}
}
