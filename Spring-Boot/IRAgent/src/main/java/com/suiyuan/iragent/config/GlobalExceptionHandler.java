package com.suiyuan.iragent.config;

import com.suiyuan.iragent.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理应用中抛出的异常，返回标准化的错误响应
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.warn("参数校验失败: uri={}, errors={}", request.getRequestURI(), errors);
        
        if (isSseRequest(request)) {
            String sseError = buildSseErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    "参数校验失败: " + errors.toString()
            );
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(sseError);
        }
        
        Map<String, Object> response = buildErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "参数校验失败",
                request.getRequestURI(),
                errors
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        
        log.warn("非法参数: uri={}, message={}", request.getRequestURI(), ex.getMessage());
        
        if (isSseRequest(request)) {
            String sseError = buildSseErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    ex.getMessage()
            );
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(sseError);
        }
        
        Map<String, Object> response = buildErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        
        log.warn("认证失败: uri={}, message={}", request.getRequestURI(), ex.getMessage());
        
        if (isSseRequest(request)) {
            String sseError = buildSseErrorResponse(
                    HttpStatus.UNAUTHORIZED.value(),
                    ex.getMessage()
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(sseError);
        }
        
        Map<String, Object> response = buildErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * 处理权限异常
     */
    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<?> handleAuthorizationException(
            AuthorizationException ex, HttpServletRequest request) {
        
        log.warn("权限不足: uri={}, message={}", request.getRequestURI(), ex.getMessage());
        
        if (isSseRequest(request)) {
            String sseError = buildSseErrorResponse(
                    HttpStatus.FORBIDDEN.value(),
                    ex.getMessage()
            );
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(sseError);
        }
        
        Map<String, Object> response = buildErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * 处理资源未找到异常
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        
        log.warn("资源未找到: uri={}, message={}", request.getRequestURI(), ex.getMessage());
        
        if (isSseRequest(request)) {
            String sseError = buildSseErrorResponse(
                    HttpStatus.NOT_FOUND.value(),
                    ex.getMessage()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(sseError);
        }
        
        Map<String, Object> response = buildErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        log.error("系统异常: uri={}", request.getRequestURI(), ex);
        
        if (isSseRequest(request)) {
            String sseError = buildSseErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "系统繁忙，请稍后重试"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(sseError);
        }
        
        Map<String, Object> response = buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "系统繁忙，请稍后重试",
                request.getRequestURI(),
                null
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 判断是否为 SSE 请求
     */
    private boolean isSseRequest(HttpServletRequest request) {
        String acceptHeader = request.getHeader("Accept");
        String contentTypeHeader = request.getHeader("Content-Type");
        
        // 检查 Accept 头是否包含 text/event-stream
        if (acceptHeader != null && acceptHeader.contains("text/event-stream")) {
            return true;
        }
        
        // 检查 Content-Type 是否为 text/event-stream
        if (contentTypeHeader != null && contentTypeHeader.contains("text/event-stream")) {
            return true;
        }
        
        // 检查请求路径是否为流式接口
        String requestUri = request.getRequestURI();
        if (requestUri != null && requestUri.contains("/stream")) {
            return true;
        }
        
        return false;
    }

    /**
     * 构建 SSE 格式的错误响应
     */
    private String buildSseErrorResponse(int code, String message) {
        // 转义特殊字符，防止 JSON 解析错误
        String escapedMessage = message.replace("\\", "\\\\")
                                      .replace("\"", "\\\"")
                                      .replace("\n", "\\n")
                                      .replace("\r", "\\r");
        return "data: {\"type\":\"error\",\"code\":" + code + ",\"message\":\"" + escapedMessage + "\"}\n\n";
    }

    /**
     * 构建标准化错误响应
     */
    private Map<String, Object> buildErrorResponse(int code, String message, 
                                                   String path, Object details) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("code", code);
        response.put("message", message);
        response.put("path", path);
        response.put("timestamp", LocalDateTime.now().toString());
        if (details != null) {
            response.put("details", details);
        }
        return response;
    }

    // ==================== 自定义异常类 ====================

    /**
     * 认证异常
     */
    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }
    }

    /**
     * 权限异常
     */
    public static class AuthorizationException extends RuntimeException {
        public AuthorizationException(String message) {
            super(message);
        }
    }

    /**
     * 资源未找到异常
     */
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }
}
