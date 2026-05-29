package com.suiyuan.iragent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.ai.volcengine")
public class AIConfiguration {

    private String apiKey;
    private String embeddingApiKey;
    private String deepseekApiKey;
    private String model;
    private String deepseekModel;
    private String baseUrl;

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        StringRedisSerializer ser = new StringRedisSerializer();
        template.setKeySerializer(ser);
        template.setValueSerializer(ser);
        template.setHashKeySerializer(ser);
        template.setHashValueSerializer(ser);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        StringRedisSerializer ser = new StringRedisSerializer();
        template.setKeySerializer(ser);
        template.setValueSerializer(ser);
        template.setHashKeySerializer(ser);
        template.setHashValueSerializer(ser);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisChatMemoryRepository redisChatMemoryRepository(RedisTemplate<String, Object> redisTemplate) {
        return new RedisChatMemoryRepository(redisTemplate);
    }

    @Bean
    public MockChatClient mockChatClient() {
        return new MockChatClient();
    }

    // ==================== 三 Key 架构 ====================

    @Bean
    @Primary
    public VolcEngineChatClient volcEngineChatClient(StringRedisTemplate redis) {
        String url = resolveBaseUrl(redis);
        return new VolcEngineChatClient(readRedisOrYaml(redis, "admin:doubao-chat-key", apiKey), model, url);
    }

    @Bean
    @Primary
    public VolcEngineStreamingClient volcEngineStreamingClient(StringRedisTemplate redis) {
        String url = resolveBaseUrl(redis);
        return new VolcEngineStreamingClient(readRedisOrYaml(redis, "admin:doubao-chat-key", apiKey), model, url);
    }

    // ==================== DeepSeek Clients (诊断/时间轴) ====================

    @Bean
    public VolcEngineChatClient deepSeekChatClient(StringRedisTemplate redis) {
        String key = readRedisOrYaml(redis, "admin:deepseek-key", deepseekApiKey);
        if (key == null || key.isBlank()) key = readRedisOrYaml(redis, "admin:doubao-chat-key", apiKey);
        return new VolcEngineChatClient(key, deepseekModel, resolveBaseUrl(redis));
    }

    @Bean
    public VolcEngineStreamingClient deepSeekStreamingClient(StringRedisTemplate redis) {
        String key = readRedisOrYaml(redis, "admin:deepseek-key", deepseekApiKey);
        if (key == null || key.isBlank()) key = readRedisOrYaml(redis, "admin:doubao-chat-key", apiKey);
        return new VolcEngineStreamingClient(key, deepseekModel, resolveBaseUrl(redis));
    }

    /** Base URL 支持热配置：Redis admin:custom-base-url → yaml */
    String resolveBaseUrl(StringRedisTemplate redis) {
        try {
            String v = redis.opsForValue().get("admin:custom-base-url");
            if (v != null && !v.isBlank()) return v;
        } catch (Exception ignored) {}
        return baseUrl;
    }

    /** 读 Redis → fallback yaml */
    String readRedisOrYaml(StringRedisTemplate redis, String redisKey, String fallback) {
        try {
            String v = redis.opsForValue().get(redisKey);
            if (v != null && !v.isBlank()) return v;
        } catch (Exception ignored) {}
        return fallback;
    }

    /** Embedding Key 独立：Redis → yaml embedding-api-key → fallback Doubao Chat Key */
    public String resolveEmbeddingKey(StringRedisTemplate redis) {
        try {
            String v = redis.opsForValue().get("admin:doubao-embedding-key");
            if (v != null && !v.isBlank()) return v;
        } catch (Exception ignored) {}
        if (embeddingApiKey != null && !embeddingApiKey.isBlank()) return embeddingApiKey;
        return readRedisOrYaml(redis, "admin:doubao-chat-key", apiKey);
    }
}
