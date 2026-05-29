package com.suiyuan.iragent.service.impl;

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suiyuan.iragent.service.FeedbackTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackTokenServiceImpl implements FeedbackTokenService {

    private static final String TOKEN_PREFIX = "feedback:token:";
    private static final long DEFAULT_EXPIRE_SECONDS = 300;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String generateToken(String sessionId, Integer stepId, String userAnswer) {
        String token = IdUtil.fastSimpleUUID();
        
        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("sessionId", sessionId);
        tokenData.put("stepId", stepId);
        tokenData.put("userAnswer", userAnswer);
        
        String key = TOKEN_PREFIX + token;
        try {
            String value = objectMapper.writeValueAsString(tokenData);
            stringRedisTemplate.opsForValue().set(key, value, DEFAULT_EXPIRE_SECONDS, TimeUnit.SECONDS);
            log.debug("生成feedbackToken: token={}, sessionId={}, stepId={}", token, sessionId, stepId);
        } catch (JsonProcessingException e) {
            log.error("Token数据序列化失败", e);
            throw new RuntimeException("Token生成失败");
        }
        
        return token;
    }

    @Override
    public Map<String, Object> getTokenData(String token) {
        String key = TOKEN_PREFIX + token;
        String value = stringRedisTemplate.opsForValue().get(key);
        
        if (value == null) {
            log.warn("Token不存在或已过期: token={}", token);
            return null;
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(value, Map.class);
            return data;
        } catch (JsonProcessingException e) {
            log.error("Token数据反序列化失败: token={}, error={}", token, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean validateToken(String token) {
        String key = TOKEN_PREFIX + token;
        Boolean exists = stringRedisTemplate.hasKey(key);
        return exists != null && exists;
    }

    @Override
    public void removeToken(String token) {
        String key = TOKEN_PREFIX + token;
        stringRedisTemplate.delete(key);
        log.debug("删除Token: token={}", token);
    }

    @Override
    public void setTokenData(String token, Map<String, Object> data, long expireSeconds) {
        String key = TOKEN_PREFIX + token;
        try {
            String value = objectMapper.writeValueAsString(data);
            stringRedisTemplate.opsForValue().set(key, value, expireSeconds, TimeUnit.SECONDS);
            log.debug("更新Token数据: token={}, expireSeconds={}", token, expireSeconds);
        } catch (JsonProcessingException e) {
            log.error("Token数据序列化失败", e);
            throw new RuntimeException("Token数据更新失败");
        }
    }
}
