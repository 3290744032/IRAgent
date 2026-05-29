package com.suiyuan.iragent.service;

import com.suiyuan.iragent.entity.Conversation;
import com.suiyuan.iragent.entity.Message;

import java.util.List;

public interface ConversationService {

    /**
     * 创建新会话
     */
    Conversation createConversation(Long userId, String name, String description);

    /**
     * 获取用户的会话列表
     */
    List<Conversation> getConversationsByUserId(Long userId);

    /**
     * 获取用户的会话列表（分页）
     */
    List<Conversation> getConversationsByUserIdWithPage(Long userId, int page, int size);

    /**
     * 统计用户的会话数量
     */
    long countConversationsByUserId(Long userId);

    /**
     * 获取用户的活跃会话列表
     */
    List<Conversation> getActiveConversationsByUserId(Long userId);

    /**
     * 获取会话详情
     */
    Conversation getConversationById(String conversationId);

    /**
     * 更新会话信息
     */
    Conversation updateConversation(Conversation conversation);

    /**
     * 更新会话名称
     */
    void updateConversationName(String conversationId, String name);

    /**
     * 删除会话
     */
    boolean deleteConversation(String conversationId);

    /**
     * 发送消息（存储到数据库）
     */
    Message sendMessage(String conversationId, String senderType, String content, String messageType);

    /**
     * 获取会话的消息列表
     */
    List<Message> getMessagesByConversationId(String conversationId);

    /**
     * 获取会话的消息列表（分页）
     */
    List<Message> getMessagesByConversationIdWithPage(String conversationId, int page, int size);

    /**
     * 统计会话的消息数量
     */
    long countMessagesByConversationId(String conversationId);

    /**
     * 获取会话的最新N条消息
     */
    List<Message> getLatestMessagesByConversationId(String conversationId, int limit);

    /**
     * 获取会话的最新消息内容
     */
    String getLatestMessageContent(String conversationId);

    /**
     * 清除会话的消息
     */
    int clearMessagesByConversationId(String conversationId);
}
