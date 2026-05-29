package com.suiyuan.iragent.config;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.suiyuan.iragent.utils.RedisConstants.CHAT_MEMORY_KEY;
import static com.suiyuan.iragent.utils.RedisConstants.CHAT_MEMORY_TTL_SECONDS;

/**
 * Redis聊天记录存储库
 * 用于在Redis中存储和管理聊天记录
 */
public class RedisChatMemoryRepository {
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisChatMemoryRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        // 配置RedisTemplate的序列化器，确保List对象能够正确序列化和反序列化
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(List.class));
    }

    /**
     * 添加消息到聊天记录
     * @param conversationId 会话ID
     * @param message 消息内容
     */
    public void add(String conversationId, String message) {
        String key = CHAT_MEMORY_KEY + conversationId;
        List<String> messages = get(conversationId, Integer.MAX_VALUE);
        messages.add(message);
        redisTemplate.opsForValue().set(key, messages, CHAT_MEMORY_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 获取聊天记录
     * @param conversationId 会话ID
     * @param maxMessages 最大消息数
     * @return 消息列表
     */
    @SuppressWarnings("unchecked")
    public List<String> get(String conversationId, int maxMessages) {
        String key = CHAT_MEMORY_KEY + conversationId;
        List<String> messages = (List<String>) redisTemplate.opsForValue().get(key);
        if (messages == null) {
            return new ArrayList<>();
        }
        if (maxMessages > 0 && messages.size() > maxMessages) {
            return messages.subList(messages.size() - maxMessages, messages.size());
        }
        return messages;
    }

    /**
     * 清除聊天记录
     * @param conversationId 会话ID
     */
    public void clear(String conversationId) {
        String key = CHAT_MEMORY_KEY + conversationId;
        redisTemplate.delete(key);
    }

    /**
     * 获取聊天会话统计信息
     * @return 会话统计信息
     */
    public ChatSessionStats getSessionStats() {
        // 这里可以实现统计逻辑，例如获取总会话数、活跃会话数等
        return new ChatSessionStats(0, 0);
    }

    /**
     * 聊天会话统计信息
     */
    public static class ChatSessionStats {
        private final int totalConversations;
        private final int activeSessions;

        public ChatSessionStats(int totalConversations, int activeSessions) {
            this.totalConversations = totalConversations;
            this.activeSessions = activeSessions;
        }

        public int getTotalConversations() {
            return totalConversations;
        }

        public int getActiveSessions() {
            return activeSessions;
        }
    }
}