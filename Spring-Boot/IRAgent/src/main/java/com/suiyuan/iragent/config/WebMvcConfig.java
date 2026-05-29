package com.suiyuan.iragent.config;

import com.suiyuan.iragent.tenant.TenantQuotaInterceptor;
import com.suiyuan.iragent.utils.LoginInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private LoginInterceptor loginInterceptor;

    @Resource
    private TenantQuotaInterceptor tenantQuotaInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 登录拦截器（先执行）
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login", "/auth/register", "/auth/getVerifiCodeImage",
                        "/ai/status", "/ai/capabilities",
                        "/timeline/**", "/static/video/**",
                        "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
                        "/doc.html", "/webjars/**", "/knife4j/**"
                )
                .order(1);

        // 多租户配额拦截器（在登录之后执行，仅限同步 LLM 路径；SSE 异步接口在 Controller 内自行管理 Semaphore）
        registry.addInterceptor(tenantQuotaInterceptor)
                .addPathPatterns("/ai/solve/**")
                .order(2);
    }
}