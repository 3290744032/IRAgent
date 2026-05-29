package com.suiyuan.iragent.utils;

/**
 * Redis常量类
 * 定义Redis中使用的各种键前缀和过期时间
 */
public class RedisConstants {

    // ==================== 认证相关 ====================
    public static final String AUTH_TOKEN_KEY = "auth:token:";
    public static final String LOGIN_CODE_TTL = "auth:code:";
    public static final int LOGIN_CODE_TTL_SECONDS = 300; // 5分钟

    // ==================== 用户相关 ====================
    public static final String USER_TOKEN_KEY = "user:token:";
    public static final int USER_TOKEN_TTL_SECONDS = 3600 * 24; // 24小时
    
    public static final String USER_INFO_KEY = "user:info:";
    public static final int USER_INFO_TTL_MINUTES = 30; // 30分钟

    // ==================== 聊天相关 ====================
    public static final String CHAT_MEMORY_KEY = "chat:memory:";
    public static final int CHAT_MEMORY_TTL_SECONDS = 3600 * 24 * 7; // 7天

    // ==================== 登录统计相关 ====================
    public static final String LOGIN_STATS_KEY_PREFIX = "login:stats:";
    public static final String LOGIN_TOKENS_KEY_PREFIX = "login:tokens:";
    public static final int LOGIN_STATS_RETENTION_DAYS = 5;

    // ==================== 限流相关 ====================
    public static final String RATE_LIMIT_KEY_PREFIX = "rate:limit:";
    public static final int RATE_LIMIT_TTL_SECONDS = 60; // 1分钟
}
