package com.suiyuan.iragent.utils;

import lombok.Data;

/**
 * 统一API响应结果类
 * 用于封装API响应数据，确保响应格式一致
 */
@Data
public class Result {
    private boolean success;
    private String message;
    private Object data;
    private Long userId;

    private Result(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    private Result(boolean success, String message, Object data, Long userId) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.userId = userId;
    }

    /**
     * 成功响应
     * @param data 响应数据
     * @return 成功响应结果
     */
    public static Result ok(Object data) {
        return new Result(true, "操作成功", data);
    }

    /**
     * 成功响应（带用户ID）
     */
    public static Result ok(Object data, Long userId) {
        return new Result(true, "操作成功", data, userId);
    }

    /**
     * 成功响应（无数据）
     * @param message 响应消息
     * @return 成功响应结果
     */
    public static Result ok(String message) {
        return new Result(true, message, null);
    }

    /**
     * 失败响应
     * @param message 错误消息
     * @return 失败响应结果
     */
    public static Result fail(String message) {
        return new Result(false, message, null);
    }
}