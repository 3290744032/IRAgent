package com.suiyuan.iragent.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 统一API响应包装类
 * 用于封装所有API响应，确保响应格式一致
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /**
     * 请求是否成功
     */
    private boolean success;

    /**
     * 响应状态码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 响应时间戳
     */
    private String timestamp;

    /**
     * 分页信息（仅分页响应时包含）
     */
    private Pagination pagination;

    /**
     * 成功响应（无数据）
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, 200, "操作成功", null, LocalDateTime.now().toString(), null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, 200, "操作成功", data, LocalDateTime.now().toString(), null);
    }

    /**
     * 成功响应（带消息和数据）
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, 200, message, data, LocalDateTime.now().toString(), null);
    }

    /**
     * 成功响应（分页）
     */
    public static <T> ApiResponse<T> success(T data, long total, int page, int size) {
        Pagination pagination = new Pagination(total, page, size);
        return new ApiResponse<>(true, 200, "操作成功", data, LocalDateTime.now().toString(), pagination);
    }

    /**
     * 失败响应（带消息）
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, 500, message, null, LocalDateTime.now().toString(), null);
    }

    /**
     * 失败响应（带状态码和消息）
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(false, code, message, null, LocalDateTime.now().toString(), null);
    }

    /**
     * 参数错误响应
     */
    public static <T> ApiResponse<T> badRequest(String message) {
        return new ApiResponse<>(false, 400, message, null, LocalDateTime.now().toString(), null);
    }

    /**
     * 未认证响应
     */
    public static <T> ApiResponse<T> unauthorized(String message) {
        return new ApiResponse<>(false, 401, message, null, LocalDateTime.now().toString(), null);
    }

    /**
     * 无权限响应
     */
    public static <T> ApiResponse<T> forbidden(String message) {
        return new ApiResponse<>(false, 403, message, null, LocalDateTime.now().toString(), null);
    }

    /**
     * 资源未找到响应
     */
    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(false, 404, message, null, LocalDateTime.now().toString(), null);
    }

    /**
     * 分页信息内部类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pagination {
        /**
         * 总记录数
         */
        private long total;

        /**
         * 当前页码（从1开始）
         */
        private int page;

        /**
         * 每页大小
         */
        private int size;

        /**
         * 总页数
         */
        public long getTotalPages() {
            return size > 0 ? (total + size - 1) / size : 0;
        }

        /**
         * 是否有下一页
         */
        public boolean hasNext() {
            return (long) page * size < total;
        }

        /**
         * 是否有上一页
         */
        public boolean hasPrevious() {
            return page > 1;
        }
    }
}