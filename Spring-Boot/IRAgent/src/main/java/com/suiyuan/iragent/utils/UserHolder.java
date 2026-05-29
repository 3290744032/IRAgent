package com.suiyuan.iragent.utils;

import com.suiyuan.iragent.entity.User;

/**
 * 用户持有类
 * 用于在请求线程中存储和获取当前用户信息
 */
public class UserHolder {
    private static final ThreadLocal<User> userThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<String> tokenThreadLocal = new ThreadLocal<>();

    /**
     * 设置当前用户
     * @param user 用户信息
     */
    public static void setUser(User user) {
        userThreadLocal.set(user);
    }

    /**
     * 获取当前用户
     * @return 用户信息
     */
    public static User getUser() {
        return userThreadLocal.get();
    }

    /**
     * 设置当前用户的token
     * @param token 用户token
     */
    public static void setToken(String token) {
        tokenThreadLocal.set(token);
    }

    /**
     * 获取当前用户的token
     * @return 用户token
     */
    public static String getToken() {
        return tokenThreadLocal.get();
    }

    /**
     * 清除当前线程的用户信息
     */
    public static void clear() {
        userThreadLocal.remove();
        tokenThreadLocal.remove();
    }
}