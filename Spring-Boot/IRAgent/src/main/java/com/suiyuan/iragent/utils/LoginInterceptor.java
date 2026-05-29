package com.suiyuan.iragent.utils;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suiyuan.iragent.entity.User;
import com.suiyuan.iragent.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

import static com.suiyuan.iragent.utils.RedisConstants.USER_TOKEN_KEY;
import static com.suiyuan.iragent.utils.RedisConstants.USER_TOKEN_TTL_SECONDS;
import static com.suiyuan.iragent.utils.RedisConstants.USER_INFO_KEY;
import static com.suiyuan.iragent.utils.RedisConstants.USER_INFO_TTL_MINUTES;

/**
 * 登录拦截器
 * 用于验证用户登录状态和Token管理
 * 优化：添加Redis用户信息缓存，减少数据库查询
 */
@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public LoginInterceptor(StringRedisTemplate stringRedisTemplate,
                            UserMapper userMapper,
                            ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求头中的token，支持Authorization和token两种header名称
        String token = request.getHeader("Authorization");
        if (StrUtil.isBlank(token)) {
            token = request.getHeader("token");
        }
        if (StrUtil.isBlank(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"code\":401,\"message\":\"未登录，请先登录\"}");
            return false;
        }

        // 从Redis中获取用户ID
        String userTokenKey = USER_TOKEN_KEY + token;
        String userIdStr = stringRedisTemplate.opsForValue().get(userTokenKey);
        if (StrUtil.isBlank(userIdStr)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"code\":401,\"message\":\"登录已过期，请重新登录\"}");
            return false;
        }

        Long userId = Long.parseLong(userIdStr);
        
        // 尝试从缓存获取用户信息（优化：减少数据库查询）
        User user = getUserFromCache(userId);
        
        if (user == null) {
            // 缓存未命中，从数据库查询
            user = userMapper.selectById(userId);
            if (user == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"code\":401,\"message\":\"用户不存在\"}");
                return false;
            }
            // 将用户信息存入缓存
            cacheUserInfo(userId, user);
            log.debug("用户信息缓存未命中，从数据库获取并缓存: userId={}", userId);
        } else {
            log.debug("用户信息缓存命中: userId={}", userId);
        }

        // 刷新Token过期时间
        stringRedisTemplate.expire(userTokenKey, USER_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);

        // 将用户信息存储到ThreadLocal
        UserHolder.setUser(user);
        UserHolder.setToken(token);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清除ThreadLocal中的用户信息
        UserHolder.clear();
    }

    /**
     * 从缓存获取用户信息
     * @param userId 用户ID
     * @return 用户信息，如果缓存未命中返回null
     */
    private User getUserFromCache(Long userId) {
        try {
            String userInfoKey = USER_INFO_KEY + userId;
            String userJson = stringRedisTemplate.opsForValue().get(userInfoKey);
            if (StrUtil.isNotBlank(userJson)) {
                return objectMapper.readValue(userJson, User.class);
            }
        } catch (JsonProcessingException e) {
            log.warn("解析缓存用户信息失败: userId={}, error={}", userId, e.getMessage());
            // 缓存数据格式错误，删除缓存
            try {
                stringRedisTemplate.delete(USER_INFO_KEY + userId);
            } catch (Exception ex) {
                log.warn("删除损坏的用户缓存失败: userId={}", userId);
            }
        } catch (Exception e) {
            log.warn("从缓存获取用户信息失败: userId={}, error={}", userId, e.getMessage());
        }
        return null;
    }

    /**
     * 将用户信息存入缓存
     * @param userId 用户ID
     * @param user 用户信息
     */
    private void cacheUserInfo(Long userId, User user) {
        try {
            String userInfoKey = USER_INFO_KEY + userId;
            String userJson = objectMapper.writeValueAsString(user);
            stringRedisTemplate.opsForValue().set(userInfoKey, userJson, USER_INFO_TTL_MINUTES, TimeUnit.MINUTES);
            log.debug("用户信息已缓存: userId={}", userId);
        } catch (JsonProcessingException e) {
            log.warn("序列化用户信息失败: userId={}, error={}", userId, e.getMessage());
        } catch (Exception e) {
            log.warn("缓存用户信息失败: userId={}, error={}", userId, e.getMessage());
        }
    }
}
