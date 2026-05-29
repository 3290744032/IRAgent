package com.suiyuan.iragent.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyProvider {

    private static final String REDIS_DOUBAO_CHAT_KEY = "admin:doubao-chat-key";
    private static final String REDIS_DEEPSEEK_KEY = "admin:deepseek-key";
    private static final String REDIS_DOUBAO_EMBEDDING_KEY = "admin:doubao-embedding-key";

    private final StringRedisTemplate redis;
    private final String yamlChatKey;
    private final String yamlDeepseekKey;
    private final String yamlEmbeddingKey;

    public ApiKeyProvider(StringRedisTemplate redis,
                          @Value("${spring.ai.volcengine.api-key}") String yamlChatKey,
                          @Value("${spring.ai.volcengine.deepseek-api-key:}") String yamlDeepseekKey,
                          @Value("${spring.ai.volcengine.embedding-api-key:}") String yamlEmbeddingKey) {
        this.redis = redis;
        this.yamlChatKey = yamlChatKey;
        this.yamlDeepseekKey = yamlDeepseekKey;
        this.yamlEmbeddingKey = yamlEmbeddingKey;
    }

    @PostConstruct
    public void migrateOldKey() {
        String oldKey = redis.opsForValue().get("admin:api-key");
        if (oldKey != null && !oldKey.isBlank()) {
            Boolean chatExists = redis.hasKey(REDIS_DOUBAO_CHAT_KEY);
            if (!Boolean.TRUE.equals(chatExists)) {
                redis.opsForValue().set(REDIS_DOUBAO_CHAT_KEY, oldKey);
            }
            redis.delete("admin:api-key");
        }
        // migrate old double-key names to new triple-key names
        migrateOldDoubleKey("admin:api-key:chat", REDIS_DOUBAO_CHAT_KEY);
        migrateOldDoubleKey("admin:api-key:embedding", REDIS_DOUBAO_EMBEDDING_KEY);
    }

    private void migrateOldDoubleKey(String oldRedisKey, String newRedisKey) {
        String oldVal = redis.opsForValue().get(oldRedisKey);
        if (oldVal != null && !oldVal.isBlank()) {
            Boolean exists = redis.hasKey(newRedisKey);
            if (!Boolean.TRUE.equals(exists)) {
                redis.opsForValue().set(newRedisKey, oldVal);
            }
            redis.delete(oldRedisKey);
        }
    }

    public String getDoubaoChatKey() {
        try {
            String redisKey = redis.opsForValue().get(REDIS_DOUBAO_CHAT_KEY);
            if (redisKey != null && !redisKey.isBlank()) return redisKey;
        } catch (Exception ignored) {}
        return yamlChatKey;
    }

    public String getDeepseekKey() {
        try {
            String redisKey = redis.opsForValue().get(REDIS_DEEPSEEK_KEY);
            if (redisKey != null && !redisKey.isBlank()) return redisKey;
        } catch (Exception ignored) {}
        return yamlDeepseekKey;
    }

    public String getEmbeddingKey() {
        try {
            String redisKey = redis.opsForValue().get(REDIS_DOUBAO_EMBEDDING_KEY);
            if (redisKey != null && !redisKey.isBlank()) return redisKey;
        } catch (Exception ignored) {}
        if (yamlEmbeddingKey != null && !yamlEmbeddingKey.isBlank()) return yamlEmbeddingKey;
        return getDoubaoChatKey();
    }

    public void refreshDoubaoChatKey(String newKey) {
        if (newKey == null || newKey.isBlank()) {
            redis.delete(REDIS_DOUBAO_CHAT_KEY);
        } else {
            redis.opsForValue().set(REDIS_DOUBAO_CHAT_KEY, newKey);
        }
    }

    public void refreshDeepseekKey(String newKey) {
        if (newKey == null || newKey.isBlank()) {
            redis.delete(REDIS_DEEPSEEK_KEY);
        } else {
            redis.opsForValue().set(REDIS_DEEPSEEK_KEY, newKey);
        }
    }

    public void refreshEmbeddingKey(String newKey) {
        if (newKey == null || newKey.isBlank()) {
            redis.delete(REDIS_DOUBAO_EMBEDDING_KEY);
        } else {
            redis.opsForValue().set(REDIS_DOUBAO_EMBEDDING_KEY, newKey);
        }
    }

    public boolean hasDoubaoChatKey() {
        String key = getDoubaoChatKey();
        return key != null && !key.isBlank() && !"your-api-key-here".equals(key);
    }

    public boolean hasDeepseekKey() {
        String key = getDeepseekKey();
        return key != null && !key.isBlank();
    }

    public boolean hasEmbeddingKey() {
        String key = getEmbeddingKey();
        return key != null && !key.isBlank();
    }

    public boolean isEmbeddingFallback() {
        try {
            String redisKey = redis.opsForValue().get(REDIS_DOUBAO_EMBEDDING_KEY);
            if (redisKey != null && !redisKey.isBlank()) return false;
        } catch (Exception ignored) {}
        return yamlEmbeddingKey == null || yamlEmbeddingKey.isBlank();
    }

    private static String mask(String key) {
        if (key == null || key.isEmpty()) return "未设置";
        return key.length() > 8
                ? key.substring(0, 4) + "****" + key.substring(key.length() - 4)
                : "已设置";
    }

    public String getMaskedDoubaoChatKey() {
        return mask(getDoubaoChatKey());
    }

    public String getMaskedDeepseekKey() {
        return mask(getDeepseekKey());
    }

    public String getMaskedEmbeddingKey() {
        return mask(getEmbeddingKey());
    }

    public String getDoubaoChatStatus() {
        return hasDoubaoChatKey() ? "正常" : "未设置";
    }

    public String getDeepseekStatus() {
        return hasDeepseekKey() ? "正常" : "未设置";
    }

    public String getEmbeddingStatus() {
        return hasEmbeddingKey() ? "正常" : isEmbeddingFallback() ? "使用 Chat Key" : "未设置";
    }
}
