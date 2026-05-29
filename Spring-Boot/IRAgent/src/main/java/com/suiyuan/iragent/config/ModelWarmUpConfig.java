package com.suiyuan.iragent.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ModelWarmUpConfig {

    private final VolcEngineStreamingClient volcEngineStreamingClient;

    /**
     * 服务启动时预热模型，减少首次请求延迟
     */
    @PostConstruct
    public void warmUpModels() {
        try {
            log.info("========== 开始模型预热 ==========");
            volcEngineStreamingClient.warmUp();
            log.info("========== 模型预热完成 ==========");
        } catch (Exception e) {
            log.warn("模型预热失败（不影响服务启动）: {}", e.getMessage());
        }
    }
}
