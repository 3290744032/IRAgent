package com.suiyuan.iragent.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suiyuan.iragent.utils.UserHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * 多租户配额拦截器——在 LLM 调用入口处检查 Semaphore 配额。
 * 仅拦截 /diagnosis/** 和 /ai/solve/** 等大模型调用路径。
 */
@Slf4j
@Component
public class TenantQuotaInterceptor implements HandlerInterceptor {

    private final TenantSemaphoreRegistry registry;
    private final ObjectMapper objectMapper;

    private static final long ACQUIRE_TIMEOUT_MS = 1000;
    private static final String[] LLM_PATHS = {"/diagnosis/", "/ai/solve/"};

    public TenantQuotaInterceptor(TenantSemaphoreRegistry registry, ObjectMapper objectMapper) {
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                              Object handler) throws Exception {
        String path = request.getRequestURI();

        boolean isLlmPath = false;
        for (String p : LLM_PATHS) {
            if (path.contains(p)) { isLlmPath = true; break; }
        }
        if (!isLlmPath) return true;

        var user = UserHolder.getUser();
        String tenantId = user != null && user.getUserId() != null
                ? "tenant-" + user.getUserId() : "anonymous";

        if (!registry.tryAcquire(tenantId, ACQUIRE_TIMEOUT_MS)) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                    Map.of("code", 429, "message", "当前使用人数过多，请稍后重试")));
            return false;
        }

        request.setAttribute("_tenantId", tenantId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                 Object handler, Exception ex) {
        String tenantId = (String) request.getAttribute("_tenantId");
        if (tenantId != null) {
            registry.release(tenantId);
        }
    }
}
